package org.giuse.CodeGenerator.parser;
import com.vp.plugin.action.VPContext;
import com.vp.plugin.diagram.*;
import com.vp.plugin.diagram.shape.IClassUIModel;
import com.vp.plugin.diagram.shape.IPackageUIModel;
import com.vp.plugin.model.*;
import org.giuse.CodeGenerator.logger.Logger;
import org.giuse.CodeGenerator.parser.models.*;
import org.giuse.CodeGenerator.parser.models.Class;
import org.giuse.CodeGenerator.parser.models.Enum;
import org.giuse.CodeGenerator.parser.models.Package;
import org.giuse.CodeGenerator.utils.ChooseListDialogHandler;
import org.giuse.CodeGenerator.utils.FormatUtils;
import org.giuse.CodeGenerator.utils.GUI;
import java.awt.*;
import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.giuse.CodeGenerator.utils.GUI.viewManager;

public class Parser {
    private enum ClassType{CLASS,INTERFACE,ENUM}
    public static final String TAG = "Parser";
    private static Parser INSTANCE;
    public static String DEFAULT_PATH;
    Codebase codebase;
    VPContext context;
    private String contextPath;
    private Boolean errorFlag;

    public static Parser getInstance(VPContext context, String choosePath){
        //TODO Config.UpdateProperty("actions.generate_code.default_path",choosePath);
        String name = context.getDiagram().getName();

        DEFAULT_PATH = choosePath;

        if(INSTANCE == null)
            INSTANCE = new Parser(context);

        INSTANCE.codebase = new Codebase(name,new ArrayList<>(), new ArrayList<>(), DEFAULT_PATH + "\\" + name);

        INSTANCE.errorFlag = false;

        return INSTANCE;
    }

    private Parser(VPContext context){
        this.context = context;
    }

    public Codebase getCodebase(){
        return this.codebase;
    }

    private Struct parseClass(IClassUIModel iClassUIModel, String parentPath, Boolean notificationEnabled){
        IClass iClass = (IClass) iClassUIModel.getModelElement();

        //endRelationship.getMasterView().getDiagramUIModel().getName()
        
        ClassType classType;
        AtomicBoolean hasExtend = new AtomicBoolean(false);
        Struct.Builder builder;

        String optionalPackagePath = parentPath;

        if(parentPath == null)
            optionalPackagePath = getElementPath(iClassUIModel);

        if(iClass.hasStereotype("Interface")){
            builder = new Interface.Builder(optionalPackagePath + "\\" + iClass.getName() + ".java", iClass.getVisibility(), iClass.getName());
            classType = ClassType.INTERFACE;
        }
        else if(iClass.hasStereotype("Enum")){
            builder = new Enum.Builder(optionalPackagePath + "\\" + iClass.getName() + ".java", iClass.getVisibility(), iClass.getName());
            classType = ClassType.ENUM;
        }
        else{
            builder = new Class.Builder(optionalPackagePath + "\\" + iClass.getName() + ".java", iClass.getVisibility(), iClass.getName());
            classType = ClassType.CLASS;
        }

        if((classType == ClassType.CLASS) && iClass.isAbstract())
            ((Class.Builder)builder).isAbstract();

        IAttribute[] attributes = iClass.toAttributeArray();
        IOperation[] operations = iClass.toOperationArray();
        IRelationshipEnd[] relationshipsFrom = iClass.toFromRelationshipEndArray();
        IRelationshipEnd[] relationshipsTo = iClass.toToRelationshipEndArray();
        ISimpleRelationship[] simpleRelationshipsFrom = iClass.toFromRelationshipArray();
        ITemplateParameter[] templateParameters = iClass.toTemplateParameterArray();
        IClass[] innerClasses = iClass.toClassArray();
        Color defaultColor = iClassUIModel.getFillColor().getColor1();

        //attributes
        for (IAttribute attribute : attributes)
            if(!handleAttributeParsing(attribute,iClassUIModel,builder,classType,defaultColor, notificationEnabled)){
                this.errorFlag = true;
                return null;
            }

        //functions
        for (IOperation operation : operations)
            if(!handleFunctionParsing(operation, iClassUIModel, builder, classType, defaultColor, notificationEnabled)){
                this.errorFlag = true;
                return null;
            }

        //template
        if((templateParameters != null) && (classType != ClassType.ENUM))
            builder.setTemplate(parseTemplate(templateParameters, iClass, notificationEnabled));

        if(parentPath != null){
            //inner classes
            for(IClass innerClass: innerClasses){
                IClassUIModel uiModel = (IClassUIModel) getUIModelFromElement(innerClass);
                if(uiModel != null) {
                    Struct parsedInnerClass = parseClass(uiModel, getElementPath(uiModel), true);

                    if(parsedInnerClass == null)
                        this.errorFlag = true;

                    if ((parsedInnerClass != null) && (parsedInnerClass.getAbsolutePath().startsWith(contextPath)))
                        builder.addInnerClass(parsedInnerClass);
                }
            }

            //attributes from relations
            for(IRelationshipEnd relationship: relationshipsFrom)
                if(!handleEndRelationshipParsing(relationship,iClassUIModel,builder,IAssociation.DIRECTION_FROM_TO, notificationEnabled)){
                    this.errorFlag = true;
                    return null;
                }

            for(IRelationshipEnd relationship: relationshipsTo)
                if(!handleEndRelationshipParsing(relationship,iClassUIModel,builder,IAssociation.DIRECTION_TO_FROM, notificationEnabled)){
                    this.errorFlag = true;
                    return null;
                }

            //inheritance
            for(ISimpleRelationship relationship: simpleRelationshipsFrom){
                switch ( handleSimpleRelationshipParsing(relationship,iClass,builder,classType,iClassUIModel,defaultColor,hasExtend, notificationEnabled)){
                    case 1:
                        this.errorFlag = true;
                        return null;
                    case 2:
                        continue;
                    default:
                }
            }
        }

        return builder.addConstructor().build();
    }

    private Integer handleSimpleRelationshipParsing(ISimpleRelationship relationship, IClass iClass, Struct.Builder builder, ClassType classType, IClassUIModel iClassUIModel, Color defaultColor, AtomicBoolean hasExtend, Boolean notificationEnabled){
        if (relationship instanceof IGeneralization) {
            IModelElement to = relationship.getTo();

            IClassUIModel uiModel = (IClassUIModel) getUIModelFromElement(to);

            if (uiModel != null) {
                Struct parsedClass = parseClass(uiModel, getElementPath(uiModel), false);

                if ((parsedClass == null) || (!parsedClass.getAbsolutePath().startsWith(contextPath)))
                    return 2;
            }

            if(!(to instanceof IClass))
                return 2; //should step on next iteration

            if(to.hasStereotype("Interface")){

                if(notificationEnabled)
                    Logger.showInfo("Parsing " + iClass.getName() + "-> " + " implements " + to.getName());

                if(classType == ClassType.INTERFACE)
                    ((Interface.Builder) builder).addExtends(to.getName());
                else if (classType == ClassType.CLASS)
                    ((Class.Builder) builder).addImplements(to.getName());
                else
                    ((Enum.Builder) builder).addImplements(to.getName());

                for(IOperation function: ((IClass) to).toOperationArray()){
                    Function parsedFunction = parseFunction(function, iClass, false);

                    if(parsedFunction != null){
                        parsedFunction.setOverride(true);

                        builder.addFunction(parsedFunction);
                    }
                    else{
                        ICompartmentColorModel attributeColor = iClassUIModel.getCompartmentColorModel(function,true);
                        attributeColor.setBackground(GUI.defaultError);

                        for(IParameter parameter :function.toParameterArray())
                            parameter.addPropertyChangeListener(evt -> attributeColor.setBackground(defaultColor));

                        function.addPropertyChangeListener(evt -> attributeColor.setBackground(defaultColor));
                        return 1;
                    }
                }
            }
            else{
                if(classType == ClassType.CLASS){
                    IClassUIModel classUIModel = (IClassUIModel) getUIModelFromElement(to);

                    if((!hasExtend.get()) && (classUIModel != null)){
                        if(notificationEnabled)
                            Logger.showInfo("Parsing " + iClass.getName() + "-> " + " extends " + to.getName());

                        Class extended = new Class.Builder("",null,classUIModel.getModelElement().getName()).build();

                        extended.setAttributes(parseExtendAttributes((IClass) classUIModel.getModelElement()));

                        ((Class.Builder) builder).setExtends(extended);
                        hasExtend.set(true);
                    }
                    else {
                        if(notificationEnabled)
                            GUI.showErrorParsingMessage(iClassUIModel,TAG,relationship, iClass.getName() + " cannot extends multiple classes");

                        return 1;
                    }
                }
                else{
                    if(notificationEnabled)
                        GUI.showErrorParsingMessage(iClassUIModel,TAG,relationship, iClass.getName() + " cannot extends classes");

                    return 1;
                }
            }
        }

        return 0;
    }

    private IShapeUIModel getUIModelFromElement(IModelElement element){
        for(IShapeUIModel shapeUIModel: context.getDiagram().toShapeUIModelArray())
            if (shapeUIModel.getModelElement().getId().compareTo(element.getId()) == 0)
                return shapeUIModel;

        return null;
    }

    private Boolean handleFunctionParsing(IOperation operation, IClassUIModel iClassUIModel, Struct.Builder builder, ClassType classType, Color defaultColor, Boolean notificationEnabled){
        Function parsedFunction = parseFunction(operation, (IClass) iClassUIModel.getModelElement(), notificationEnabled);

        if(parsedFunction != null){
            if((classType == ClassType.INTERFACE))
                parsedFunction.setVirtual(true);

            builder.addFunction(parsedFunction);
        }
        else{
            ICompartmentColorModel attributeColor = iClassUIModel.getCompartmentColorModel(operation,true);
            attributeColor.setBackground(GUI.defaultError);

            for(IParameter parameter :operation.toParameterArray())
                parameter.addPropertyChangeListener(evt -> attributeColor.setBackground(defaultColor));

            operation.addPropertyChangeListener(evt -> attributeColor.setBackground(defaultColor));
            return false;
        }

        return true;
    }

    private Boolean handleEndRelationshipParsing(IRelationshipEnd relationship, IClassUIModel iClassUIModel, Struct.Builder builder, int direction, Boolean notificationEnabled){
        IEndRelationship endRelationship = relationship.getEndRelationship();

        if(endRelationship instanceof IAssociation){

            Attribute parsedAssociation = parseAssociation(relationship, (IClass) iClassUIModel.getModelElement(),direction, notificationEnabled);

            if(parsedAssociation != null) {
                IClassUIModel uiModel = (IClassUIModel) getUIModelFromElement(getToModelElement(relationship, direction));

                if (uiModel != null) {
                    Struct parsedClass = parseClass(uiModel, null, false);

                    if ((parsedClass != null) && (parsedClass.getAbsolutePath().startsWith(contextPath)))
                        builder.addAttribute(parsedAssociation);
                }
            }
            else{
                iClassUIModel.setForeground(GUI.defaultError);
                endRelationship.addPropertyChangeListener(evt -> iClassUIModel.setForeground(new Color(0,0,0)));
                return false;
            }
        }

        return true;
    }

    private Boolean handleAttributeParsing(IAttribute attribute, IClassUIModel iClassUIModel, Struct.Builder builder, ClassType classType, Color defaultColor, Boolean notificationEnabled){
        Attribute parsedAttribute = parseAttribute(attribute, (IClass) iClassUIModel.getModelElement(),notificationEnabled);

        if(parsedAttribute != null){
            if(classType != ClassType.ENUM)
                builder.addAttribute(parsedAttribute);
            else{
                if(FormatUtils.isCapsLock(parsedAttribute.getName()))
                    ((Enum.Builder)builder).addPairs(parsedAttribute.getName(), parsedAttribute.getInitializer());
                else
                    builder.addAttribute(parsedAttribute);
            }
        }
        else{
            ICompartmentColorModel attributeColor = iClassUIModel.getCompartmentColorModel(attribute,true);
            attributeColor.setBackground(GUI.defaultError);
            attribute.addPropertyChangeListener(evt -> attributeColor.setBackground(defaultColor));
            return false;
        }

        return true;
    }

    private Template parseTemplate(ITemplateParameter[] parameters, IClass iClass,Boolean notificationEnabled) {
        Template.Builder builder= new Template.Builder();

        for(ITemplateParameter parameter : parameters){
            String formattedType = null;

            if(parameter.typeCount() > 0)
                 formattedType = FormatUtils.toJavaType(parameter.getTypeByIndex(0).getTypeAsString());

            if (parameter.typeCount() > 1 && notificationEnabled)
                Logger.showWarning("Parsing " + iClass.getName() + "-> " + "In " + parameter.getName() + " only first type is considered -> " +Arrays.toString(parameter.toTypeArray()));

            builder.addParameter(parameter.getName(), formattedType);

            if(parameter.getDefaultValue() != null && notificationEnabled)
                Logger.showWarning("Parsing " + iClass.getName() + "-> " + parameter.getName() +" initial value is ignored");
        }

        return builder.build();
    }

    private ArrayList<Attribute> parseExtendAttributes(IClass aClass){
        ArrayList<Attribute> attributesList = new ArrayList<>();
        IAttribute[] attributes = aClass.toAttributeArray();

        for(IAttribute attribute: attributes)
            attributesList.add(parseAttribute(attribute, aClass, false));

        return attributesList;
    }

    private Attribute parseAttribute(IAttribute attribute, IClass iClass, Boolean notificationEnabled){
        if(attribute.getTypeAsString() != null) {
            if(notificationEnabled)
                Logger.showInfo("Parsing " + iClass.getName() + "-> " + attribute.getVisibility()+" "+attribute.getTypeAsString() +" "+ attribute.getName() + ";");

            String formattedType = FormatUtils.toJavaType(attribute.getTypeAsString());

            return new Attribute(attribute.getVisibility(), formattedType, attribute.getName(), attribute.getInitialValue());
        }
        else{
            if(notificationEnabled)
                Logger.showError("Parsing " + iClass.getName() + "-> " + attribute.getName() +" has null type");

            return null;
        }
    }

    private Function parseFunction(IOperation function, IClass iClass, Boolean notificationEnabled){
        if(function.getReturnTypeAsString() != null){

            if(notificationEnabled)
                Logger.showInfo("Parsing " + iClass.getName() + "-> " + function.getVisibility() +" "+ function.getReturnTypeAsString() +" "+ function.getName() + "()");

            String returnType = function.getReturnTypeAsString();

            String formattedReturnType = returnType.compareTo("void") == 0 ? returnType : FormatUtils.toJavaType(returnType);

            Function.Builder builderFunction = new Function.Builder(function.getName(),function.getVisibility(), formattedReturnType);

            for(IParameter parameter :function.toParameterArray()){
                if(parameter.getTypeAsText() !=null){
                    String formattedType = FormatUtils.toJavaType(parameter.getTypeAsString());
                    builderFunction.addParameter(new Attribute("", formattedType,parameter.getName(), ""));
                }
                else{
                    if(notificationEnabled)
                        Logger.showError("Parsing " + iClass.getName() + "-> " + "parameter " + parameter.getName() +" has null type");

                    return null;
                }
            }

            return builderFunction.build();
        }
        else{
            if(notificationEnabled)
                Logger.showError("Parsing " + iClass.getName() + "-> " + function.getName() +" has null return type");

            return null;
        }
    }

    private IModelElement getToModelElement(IRelationshipEnd relationshipEnd, int direction){
        if(direction == IAssociation.DIRECTION_FROM_TO){
            return relationshipEnd.getEndRelationship().getTo();
        }
        else if(direction == IAssociation.DIRECTION_TO_FROM){
            return relationshipEnd.getEndRelationship().getFrom();
        }
        else{
            throw new InvalidParameterException("direction parameter must be in range [0,1]");
        }
    }

    private IModelElement getFromModelElement(IRelationshipEnd relationshipEnd, int direction){
        if(direction == IAssociation.DIRECTION_FROM_TO){
            return relationshipEnd.getEndRelationship().getFrom();
        }
        else if(direction == IAssociation.DIRECTION_TO_FROM){
            return relationshipEnd.getEndRelationship().getTo();
        }
        else{
            throw new InvalidParameterException("direction parameter must be in range [0,1]");
        }
    }

    private Attribute parseAssociation(IRelationshipEnd association, IClass iClass, int direction, Boolean notificationEnabled){
        String toMultiplicity = ((IAssociationEnd) association.getOppositeEnd()).getMultiplicity();
        String fromMultiplicity = ((IAssociationEnd) association).getMultiplicity();
        boolean needsAssociationClass = toMultiplicity.contains("*") && fromMultiplicity.contains("*");
        String associationName = association.getEndRelationship().getName();

        if(needsAssociationClass && (associationName == null || associationName.isEmpty()) && notificationEnabled){
            Logger.showError("Parsing " + iClass.getName() + "-> " + "many to many association needs name");
            return null;
        }

        IModelElement from, to;
        from = getFromModelElement(association, direction);
        to = getToModelElement(association, direction);

        String aggregationKind = association.getModelPropertyByName("aggregationKind").getValueAsString();

        if(aggregationKind.compareTo("Shared") == 0 && notificationEnabled)
            Logger.showWarning("Parsing " + iClass.getName() + "-> " + "Aggregation relation between " + from.getName() + " and " + to.getName() + " is treated as association");
        else if(aggregationKind.compareTo("Composited") == 0 && notificationEnabled)
            Logger.showWarning("Parsing " + iClass.getName() + "-> " + "Composition relation between " + from.getName() + " and " + to.getName() + " is treated as association");

        if(toMultiplicity.compareTo("Unspecified") == 0 && notificationEnabled){
            Logger.showError("Parsing " + iClass.getName() + "-> " + "has no multiplicity specified for " + to.getName());
            return null;
        }

        if(notificationEnabled)
            Logger.showInfo("Parsing " + iClass.getName() + "-> " + "has " + toMultiplicity + " " + to.getName());

        String attributeType;
        String initializer = null;
        String formattedType = needsAssociationClass ? FormatUtils.toJavaType(associationName) : FormatUtils.toJavaType(to.getName());
        String attributeName = to.getName();

        if(toMultiplicity.compareTo("0") == 0)
            return null;

        if(FormatUtils.isArrayList(toMultiplicity)){
            String typeList = "ArrayList";

            if((notificationEnabled) && (!ChooseListDialogHandler.applyAlways) && (!this.errorFlag)){
                ChooseListDialogHandler chooseListDialogHandler = new ChooseListDialogHandler(from.getName(), to.getName());
                viewManager.showDialog(chooseListDialogHandler);
                typeList = chooseListDialogHandler.getChoose();

            }

            attributeType = typeList + "<" + formattedType + ">";
            attributeName+="s";
        }
        else if(FormatUtils.isFixedArray(toMultiplicity)){
            initializer = "new " + formattedType + "[" + FormatUtils.getFixedArrayLength(toMultiplicity) + "]";
            attributeType = formattedType+"[]";
        }
        else if(FormatUtils.isNotArray(toMultiplicity))
            attributeType = formattedType;
        else {
            if(notificationEnabled)
                Logger.showError("Parsing " + iClass.getName() + "-> " + toMultiplicity + " is invalid multiplicity ");

            return null;
        }

        String relationVisibility = ((IAssociation) association.getEndRelationship()).getVisibility();

        String scope = relationVisibility.compareTo("Unspecified") == 0 ? "private" : relationVisibility;

        return new Attribute(scope, attributeType, attributeName.toLowerCase(), initializer);
    }

    private Package parsePackageBottomUp(IPackageUIModel iPackage){
        IDiagramElement parent = iPackage.getParent();
        IModelElement modelParent = parent != null ? parent.getModelElement() : null;
        Package parentPackage = null;

        if(modelParent instanceof IPackage)
            parentPackage = parsePackageBottomUp((IPackageUIModel) parent);

        String optionalPackagePath = parentPackage != null ? parentPackage.getPathname() : codebase.getPathname();

        return new Package(iPackage.getModelElement().getName(), new ArrayList<>(), optionalPackagePath+"\\"+iPackage.getModelElement().getName());
    }

    private String getElementPath(IShapeUIModel modelElement){
        IDiagramElement parent = modelElement.getParent();
        IModelElement parentModel = parent != null ? parent.getModelElement() : null;
        Package parentPackage = null;

        if(parentModel instanceof IPackage)
            parentPackage = parsePackageBottomUp((IPackageUIModel) parent);

        return parentPackage != null ? parentPackage.getPathname() : codebase.getPathname();
    }


    private Package parsePackage(IPackageUIModel iPackage, String parentPath){
        Package aPackage = new Package(iPackage.getModelElement().getName(), new ArrayList<>(), parentPath + "\\" + iPackage.getModelElement().getName());

        for(IShapeUIModel shapeUIModel: iPackage.toChildArray()){
            IModelElement iModelElement = shapeUIModel.getModelElement();

            if(iModelElement instanceof IClass)
                aPackage.addFile(parseClass((IClassUIModel) shapeUIModel, aPackage.getPathname(), true));
            else if (iModelElement instanceof IPackage)
                codebase.addPackage(parsePackage((IPackageUIModel) shapeUIModel, aPackage.getPathname()));
        }

        return aPackage.getFiles().contains(null) ? null : aPackage;
    }

    private Package parseDefaultPackage(IPackage iPackage){
        Package aPackage = new Package(iPackage.getName(), new ArrayList<>(), codebase.getPathname()+"\\"+iPackage.getName());

        for(IModelElement modelElement: iPackage.toChildArray()){
            if(modelElement instanceof IClass)
                aPackage.addFile(parseClass((IClassUIModel) getUIModelFromElement(modelElement), aPackage.getPathname(),true));
            else if (modelElement instanceof IPackage)
                codebase.addPackage(parsePackage((IPackageUIModel) getUIModelFromElement(modelElement), aPackage.getPathname()));
        }

        return aPackage.getFiles().contains(null) ? null : aPackage;
    }

    public void parseDiagram(IDiagramUIModel iDiagramUIModel){
        contextPath = codebase.getPathname();
        int handled = 0;

        for(IShapeUIModel shapeUIModel: iDiagramUIModel.toShapeUIModelArray()){
            IModelElement modelElement = shapeUIModel.getModelElement();
            IModelElement parent = modelElement.getParent();

            if((parent instanceof IModel && (shapeUIModel.getParent() == null)) && (handled++ == 1))
                GUI.showWarningMessageDialog(viewManager.getRootFrame(), TAG, "Default package is not defined.");

            // TODO null pointer when switch between diagrams Logger.Debug.showMessage(shapeUIModel.getModelElement().getName(), TAG);

            if(modelElement instanceof IPackage){
                if(parent instanceof IModel && (shapeUIModel.getParent() == null))
                    codebase.addPackage(parsePackage((IPackageUIModel) shapeUIModel, codebase.getPathname()));
                else if((parent instanceof IPackage) && (shapeUIModel.getParent() == null)) {
                    codebase.addPackage(parseDefaultPackage((IPackage) parent));
                    break;
                }
            }
            else if(modelElement instanceof IClass){
                if(parent instanceof IModel && (shapeUIModel.getParent() == null))
                    codebase.addFile(parseClass((IClassUIModel) shapeUIModel, codebase.getPathname(), true));
                else if((parent instanceof IPackage) && (shapeUIModel.getParent() == null)) {
                    codebase.addPackage(parseDefaultPackage((IPackage) parent));
                    break;
                }
            }
        }
    }

    public void parseSingleClass(IClassUIModel iClass){
        contextPath = getElementPath(iClass) + "\\" + iClass.getModelElement().getName() + ".java";
        File aClass = parseClass(iClass, codebase.getPathname(), true);

        codebase.addFile(aClass);
    }

    public void parseSinglePackage(IPackageUIModel iPackage){
        contextPath = getElementPath(iPackage) + "\\" + iPackage.getModelElement().getName();
        Package aPackage = parsePackage(iPackage, codebase.getPathname());

        codebase.addPackage(aPackage);
    }
}