package org.giuse.codegenerator.parser;
import com.vp.plugin.action.VPContext;
import com.vp.plugin.diagram.*;
import com.vp.plugin.diagram.shape.IClassUIModel;
import com.vp.plugin.diagram.shape.IPackageUIModel;
import com.vp.plugin.model.*;
import org.giuse.codegenerator.logger.Logger;
import org.giuse.codegenerator.parser.models.*;
import org.giuse.codegenerator.parser.models.Class;
import org.giuse.codegenerator.parser.models.Enum;
import org.giuse.codegenerator.parser.models.Package;
import org.giuse.codegenerator.parser.models.statements.Return;
import org.giuse.codegenerator.utils.ChooseListDialogHandler;
import org.giuse.codegenerator.utils.FormatUtils;
import org.giuse.codegenerator.utils.GUI;
import java.awt.*;
import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.giuse.codegenerator.utils.GUI.viewManager;

public class Parser {
    private enum ClassType{CLASS,INTERFACE,ENUM}
    public static final String TAG = "Parser";
    private static Parser INSTANCE;
    public static String DEFAULT_PATH;
    Codebase codebase;
    VPContext context;
    private String contextPath;
    private Boolean errorFlag;
    private Logger logger;
    private String defaultPackage;

    public static Parser getInstance(VPContext context, String choosePath){
        //TODO Config.UpdateProperty("actions.generate_code.default_path",choosePath);
        String name = context.getDiagram().getName();

        DEFAULT_PATH = choosePath;

        if(INSTANCE == null)
            INSTANCE = new Parser();

        INSTANCE.context = context;

        INSTANCE.codebase = new Codebase(name,new ArrayList<>(), new ArrayList<>(), DEFAULT_PATH + File.separator + name);

        INSTANCE.logger = new Logger();

        INSTANCE.errorFlag = false;

        INSTANCE.defaultPackage = null;

        return INSTANCE;
    }

    public Codebase getCodebase(){
        return this.codebase;
    }

    private Struct parseClass(IClassUIModel iClassUIModel, String parentPath){
        IClass iClass = (IClass) iClassUIModel.getModelElement();

        //endRelationship.getMasterView().getDiagramUIModel().getName()

        ClassType classType;
        AtomicBoolean hasExtend = new AtomicBoolean(false);
        Struct.Builder builder;

        String optionalPackagePath = parentPath;

        if(parentPath == null)
            optionalPackagePath = getElementPath(iClassUIModel);

        if(iClass.hasStereotype("Interface")){
            builder = new Interface.Builder(optionalPackagePath + File.separator + iClass.getName() + ".java", iClass.getVisibility(), iClass.getName());
            classType = ClassType.INTERFACE;
        }
        else if(iClass.hasStereotype("Enum")){
            builder = new Enum.Builder(optionalPackagePath + File.separator + iClass.getName() + ".java", iClass.getVisibility(), iClass.getName());
            classType = ClassType.ENUM;
        }
        else{
            builder = new Class.Builder(optionalPackagePath + File.separator + iClass.getName() + ".java", iClass.getVisibility(), iClass.getName());
            classType = ClassType.CLASS;
        }


        builder.setPackage(getClassPackage(iClass));

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
            if(!handleAttributeParsing(attribute, iClassUIModel, builder, classType, defaultColor)){
                this.errorFlag = true;
                return null;
            }

        //functions
        for (IOperation operation : operations)
            if(!handleFunctionParsing(operation, iClassUIModel, builder, classType, defaultColor)){
                this.errorFlag = true;
                return null;
            }

        //template
        if((templateParameters != null) && (classType != ClassType.ENUM))
            builder.setTemplate(parseTemplate(templateParameters, iClass));

        if(parentPath != null){
            //inner classes
            for(IClass innerClass: innerClasses){
                IClassUIModel uiModel = (IClassUIModel) getUIModelFromElement(innerClass);

                if(uiModel != null){
                    IDiagramElement parent = uiModel.getParent();

                    if
                    (
                            ((parent != null) && (parent.getModelElement().getAddress().startsWith(contextPath)))
                            ||
                            ((parent == null) && (contextPath.isEmpty()))
                    )
                    {
                        Struct parsedInnerClass = parseClass(uiModel, getElementPath(uiModel));

                        if(parsedInnerClass == null)
                            this.errorFlag = true;
                        else
                            builder.addInnerClass(parsedInnerClass);

                        logger.queueInfoMessage(iClass.getName() + " contains declaration of " + innerClass.getName());
                    }
                }
            }

            //attributes from relations
            for(IRelationshipEnd relationship: relationshipsFrom)
                if(!handleEndRelationshipParsing(relationship,iClassUIModel,builder,IAssociation.DIRECTION_FROM_TO)){
                    this.errorFlag = true;
                    return null;
                }

            for(IRelationshipEnd relationship: relationshipsTo)
                if(!handleEndRelationshipParsing(relationship,iClassUIModel,builder,IAssociation.DIRECTION_TO_FROM)){
                    this.errorFlag = true;
                    return null;
                }

            //inheritance
            for(ISimpleRelationship relationship: simpleRelationshipsFrom){
                switch ( handleSimpleRelationshipParsing(relationship,iClass,builder,classType,iClassUIModel,defaultColor,hasExtend)){
                    case 1:
                        this.errorFlag = true;
                        return null;
                    case 2:
                        continue;
                    default:
                }
            }
        }

        return builder.build();
    }

    private Integer handleSimpleRelationshipParsing(ISimpleRelationship relationship, IClass iClass, Struct.Builder builder, ClassType classType, IClassUIModel iClassUIModel, Color defaultColor, AtomicBoolean hasExtend){
        if (relationship instanceof IGeneralization) {
            IModelElement to = relationship.getTo();

            if (!(to instanceof IClass) || !(to.getAddress().startsWith(contextPath)))
                return 2; //should step on next iteration

            if(to.hasStereotype("Interface")){

                logger.queueInfoMessage("Parsing " + iClass.getName() + "-> " + " implements " + to.getName());

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
                    if((!hasExtend.get())){
                        logger.queueInfoMessage("Parsing " + iClass.getName() + "-> " + " extends " + to.getName());

                        Class extended = new Class.Builder("",null, to.getName()).build();

                        extended.setAttributes(parseExtendAttributes((IClass) to));

                        ((Class.Builder) builder).setExtends(extended);
                        hasExtend.set(true);
                    }
                    else {
                        GUI.showErrorParsingMessage(logger, iClassUIModel,relationship, iClass.getName() + " cannot extends multiple classes");

                        return 1;
                    }
                }
                else{
                    GUI.showErrorParsingMessage(logger, iClassUIModel,relationship, iClass.getName() + " cannot extends classes");

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

    private String getNameFromAddress(String address){
        for(IShapeUIModel shapeUIModel: context.getDiagram().toShapeUIModelArray()) {
            IModelElement modelElement = shapeUIModel.getModelElement();

            if (modelElement.getId().compareTo(address) == 0)
                return modelElement.getName();
        }

        return null;
    }

    private String getClassPackage(IClass iClass){
        StringBuilder stringBuilder = new StringBuilder();
        int begin, start;
        String contextAddress;

        if(iClass.getAddress().compareTo(this.contextPath) == 0)
            return null;

        if(this.contextPath.isEmpty()){
           contextAddress = iClass.getAddress();

            if(this.defaultPackage != null) {
                stringBuilder.append(this.defaultPackage);

                if(iClass.getParent().getName().compareTo(defaultPackage) != 0)
                    stringBuilder.append(".");

                start = 2;
            }
            else
                start = 1;

            viewManager.showMessage(iClass.getName() +": "+ contextAddress, TAG);
        }
        else {
            start = 0;
            begin = this.contextPath.length() + 1;
            String[] contextPathParts = this.contextPath.split(":");
            contextAddress = contextPathParts[contextPathParts.length-1] + ":" + iClass.getAddress().substring(begin);
        }

        String[] addressParts = contextAddress.split(":");

        for(int i=start; i < addressParts.length -1 ; i++) {
            stringBuilder.append(getNameFromAddress(addressParts[i]));

            if (i < (addressParts.length - 2))
                stringBuilder.append(".");
        }

        return stringBuilder.toString();
    }

    private Boolean handleFunctionParsing(IOperation operation, IClassUIModel iClassUIModel, Struct.Builder builder, ClassType classType, Color defaultColor){
        Function parsedFunction = parseFunction(operation, (IClass) iClassUIModel.getModelElement(), true);

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

    private Boolean handleEndRelationshipParsing(IRelationshipEnd relationship, IClassUIModel iClassUIModel, Struct.Builder builder, int direction){
        IEndRelationship endRelationship = relationship.getEndRelationship();

        if(endRelationship instanceof IAssociation){
            if(getToModelElement(relationship, direction).getAddress().startsWith(contextPath)){
                Attribute parsedAssociation = parseAssociation(relationship, (IClass) iClassUIModel.getModelElement(),direction);

                if (parsedAssociation != null)
                    builder.addAttribute(parsedAssociation);
                else{
                    iClassUIModel.setForeground(GUI.defaultError);
                    endRelationship.addPropertyChangeListener(evt -> iClassUIModel.setForeground(new Color(0,0,0)));
                    return false;
                }
            }
        }

        return true;
    }

    private Boolean handleAttributeParsing(IAttribute attribute, IClassUIModel iClassUIModel, Struct.Builder builder, ClassType classType, Color defaultColor){
        Attribute parsedAttribute = parseAttribute(attribute, (IClass) iClassUIModel.getModelElement(), true);

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

    private Template parseTemplate(ITemplateParameter[] parameters, IClass iClass) {
        Template.Builder builder= new Template.Builder();

        for(ITemplateParameter parameter : parameters){
            String formattedType = null;

            if(parameter.typeCount() > 0)
                 formattedType = FormatUtils.toJavaType(parameter.getTypeByIndex(0).getTypeAsString());

            if (parameter.typeCount() > 1)
                logger.queueWarningMessage("Parsing " + iClass.getName() + "-> " + "In " + parameter.getName() + " only first type is considered -> " +Arrays.toString(parameter.toTypeArray()));

            builder.addParameter(parameter.getName(), formattedType);

            if(parameter.getDefaultValue() != null)
                logger.queueWarningMessage("Parsing " + iClass.getName() + "-> " + parameter.getName() +" initial value is ignored");
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

    private Attribute parseAttribute(IAttribute attribute, IClass iClass, Boolean notify){
        if(iClass.hasStereotype("Interface") && (attribute.getInitialValue() == null)){
            if(notify)
                logger.queueErrorMessage("Parsing " + iClass.getName() + "-> " + attribute.getName() +" must have initializer on interface");

            return null;
        }


        if(attribute.getTypeAsString() != null) {
            if(notify)
                logger.queueInfoMessage("Parsing " + iClass.getName() + "-> " + attribute.getVisibility()+" "+attribute.getTypeAsString() +" "+ attribute.getName() + ";");

            String formattedType = FormatUtils.toJavaType(attribute.getTypeAsString());

            String visibility = attribute.getVisibility();

            if((attribute.getVisibility() != null) && notify)
                logger.queueWarningMessage("Parsing " + iClass.getName() + "-> " + attribute.getName() +" visibility is ignored");

            String formattedVisibility = iClass.hasStereotype("Interface") ? null : visibility;

            return new Attribute(formattedVisibility, formattedType, attribute.getName(), attribute.getInitialValue());
        }
        else{
            if(notify)
                logger.queueErrorMessage("Parsing " + iClass.getName() + "-> " + attribute.getName() +" has null type");

            return null;
        }
    }

    private Function parseFunction(IOperation function, IClass iClass, Boolean notify){
        if(function.getReturnTypeAsString() != null){
            if(notify)
                logger.queueInfoMessage("Parsing " + iClass.getName() + "-> " + function.getVisibility() +" "+ function.getReturnTypeAsString() +" "+ function.getName() + "()");

            String returnType = function.getReturnTypeAsString();

            String formattedType = FormatUtils.toJavaType(returnType);

            String visibility = function.getVisibility();

            if((function.getVisibility() != null) && notify)
                logger.queueWarningMessage("Parsing " + iClass.getName() + "-> " + function.getName() +" visibility is ignored");

            String formattedVisibility = iClass.hasStereotype("Interface") ? null : visibility;

            Function.Builder builderFunction = new Function.Builder(function.getName(), formattedVisibility, formattedType);

            if(formattedType.compareTo("void") != 0)
                builderFunction.addStatement(new Return("null"));

            for(IParameter parameter :function.toParameterArray()){
                if(parameter.getTypeAsText() !=null){
                    String paramFormattedType = FormatUtils.toJavaType(parameter.getTypeAsString());
                    builderFunction.addParameter(new Attribute("", paramFormattedType,parameter.getName(), null));
                }
                else{
                    if(notify)
                        logger.queueErrorMessage("Parsing " + iClass.getName() + "-> " + "parameter " + parameter.getName() +" has null type");

                    return null;
                }
            }

            return builderFunction.build();
        }
        else{
            if(notify)
                logger.queueErrorMessage("Parsing " + iClass.getName() + "-> " + function.getName() +" has null return type");

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

    private Attribute parseAssociation(IRelationshipEnd association, IClass iClass, int direction){
        String toMultiplicity = ((IAssociationEnd) association.getOppositeEnd()).getMultiplicity();
        String fromMultiplicity = ((IAssociationEnd) association).getMultiplicity();
        boolean needsAssociationClass = toMultiplicity.contains("*") && fromMultiplicity.contains("*");
        String associationName = association.getEndRelationship().getName();

        if(needsAssociationClass && (associationName == null || associationName.isEmpty())){
            logger.queueErrorMessage("Parsing " + iClass.getName() + "-> " + "many to many association needs name");
            return null;
        }

        IModelElement from, to;
        from = getFromModelElement(association, direction);
        to = getToModelElement(association, direction);

        String aggregationKind = association.getModelPropertyByName("aggregationKind").getValueAsString();

        if(aggregationKind.compareTo("Shared") == 0)
            logger.queueWarningMessage("Parsing " + iClass.getName() + "-> " + "Aggregation relation between " + from.getName() + " and " + to.getName() + " is treated as association");
        else if(aggregationKind.compareTo("Composited") == 0)
            logger.queueWarningMessage("Parsing " + iClass.getName() + "-> " + "Composition relation between " + from.getName() + " and " + to.getName() + " is treated as association");

        if(toMultiplicity.compareTo("Unspecified") == 0){
            logger.queueErrorMessage("Parsing " + iClass.getName() + "-> " + "has no multiplicity specified for " + to.getName());
            return null;
        }

        logger.queueInfoMessage("Parsing " + iClass.getName() + "-> " + "has " + toMultiplicity + " " + to.getName());

        String attributeType;
        String initializer = null;
        String formattedType = needsAssociationClass ? FormatUtils.toJavaType(associationName) : FormatUtils.toJavaType(to.getName());
        String attributeName = to.getName();

        if(toMultiplicity.compareTo("0") == 0)
            return null;

        if(FormatUtils.isArrayList(toMultiplicity)){
            String typeList = "ArrayList";

            if((!ChooseListDialogHandler.applyAlways) && (!this.errorFlag)){
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
            logger.queueErrorMessage("Parsing " + iClass.getName() + "-> " + toMultiplicity + " is invalid multiplicity ");

            return null;
        }

        String relationVisibility = ((IAssociation) association.getEndRelationship()).getVisibility();

        String scope;

        if(from.hasStereotype("Interface")){
            logger.queueWarningMessage("Parsing " + iClass.getName() + "-> " + to.getName() + " visibility is ignored");
            scope = null;

            if(initializer == null)
                initializer = "null";

        }
        else{
            scope = (relationVisibility.compareTo("Unspecified") == 0) ? "private" : relationVisibility;
        }

        return new Attribute(scope, attributeType, attributeName.toLowerCase(), initializer);
    }

    private Package parsePackageBottomUp(IPackageUIModel iPackage){
        IDiagramElement parent = iPackage.getParent();
        IModelElement modelParent = parent != null ? parent.getModelElement() : null;
        Package parentPackage = null;

        if(modelParent instanceof IPackage)
            parentPackage = parsePackageBottomUp((IPackageUIModel) parent);

        String optionalPackagePath = parentPackage != null ? parentPackage.getPathname() : codebase.getPathname();

        return new Package(iPackage.getModelElement().getName(), new ArrayList<>(), optionalPackagePath + File.separator + iPackage.getModelElement().getName());
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
        Package aPackage = new Package(iPackage.getModelElement().getName(), new ArrayList<>(), parentPath + File.separator + iPackage.getModelElement().getName());

        for(IShapeUIModel shapeUIModel: iPackage.toChildArray()){
            IModelElement iModelElement = shapeUIModel.getModelElement();

            if(iModelElement.getParent() instanceof IClass)
                continue;

            if(iModelElement instanceof IClass)
                aPackage.addFile(parseClass((IClassUIModel) shapeUIModel, aPackage.getPathname()));
            else if (iModelElement instanceof IPackage)
                codebase.addPackage(parsePackage((IPackageUIModel) shapeUIModel, aPackage.getPathname()));
        }

        return aPackage.getFiles().contains(null) ? null : aPackage;
    }

    private Package parseDefaultPackage(IPackage iPackage){
        this.defaultPackage = iPackage.getName();

        Package aPackage = new Package(iPackage.getName(), new ArrayList<>(), codebase.getPathname() + File.separator + iPackage.getName());

        for(IModelElement modelElement: iPackage.toChildArray()){
            if(modelElement instanceof IClass)
                aPackage.addFile(parseClass((IClassUIModel) getUIModelFromElement(modelElement), aPackage.getPathname()));
            else if (modelElement instanceof IPackage)
                codebase.addPackage(parsePackage((IPackageUIModel) getUIModelFromElement(modelElement), aPackage.getPathname()));
        }

        return aPackage.getFiles().contains(null) ? null : aPackage;
    }

    public void parseDiagram(IDiagramUIModel iDiagramUIModel){
        contextPath = "";
        int handled = 0;

        for(IShapeUIModel shapeUIModel: iDiagramUIModel.toShapeUIModelArray()){
            IModelElement modelElement = shapeUIModel.getModelElement();
            IModelElement parent = modelElement.getParent();

            if((parent instanceof IModel && (shapeUIModel.getParent() == null)) && (handled++ == 1))
                GUI.showWarningMessageDialog(viewManager.getRootFrame(), TAG, "Default package is not defined.");


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
                    codebase.addFile(parseClass((IClassUIModel) shapeUIModel, codebase.getPathname()));
                else if((parent instanceof IPackage) && (shapeUIModel.getParent() == null)) {
                    codebase.addPackage(parseDefaultPackage((IPackage) parent));
                    break;
                }
            }
        }

        logger.consumeQueue();
    }

    public void parseSingleClass(IClassUIModel iClass){
        contextPath = iClass.getModelElement().getAddress();
        File aClass = parseClass(iClass, codebase.getPathname());

        codebase.addFile(aClass);

        logger.consumeQueue();
    }

    public void parseSinglePackage(IPackageUIModel iPackage){
        contextPath = iPackage.getModelElement().getAddress();
        Package aPackage = parsePackage(iPackage, codebase.getPathname());

        codebase.addPackage(aPackage);

        logger.consumeQueue();
    }
}