package org.giuse.CodeGenerator.parser;
import com.vp.plugin.action.VPContext;
import com.vp.plugin.diagram.*;
import com.vp.plugin.diagram.shape.IClassUIModel;
import com.vp.plugin.diagram.shape.IPackageUIModel;
import com.vp.plugin.model.*;
import org.giuse.CodeGenerator.parser.models.*;
import org.giuse.CodeGenerator.parser.models.Class;
import org.giuse.CodeGenerator.parser.models.Enum;
import org.giuse.CodeGenerator.parser.models.Package;
import org.giuse.CodeGenerator.utils.FormatUtils;
import org.giuse.CodeGenerator.utils.GUI;
import java.awt.*;
import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.giuse.CodeGenerator.utils.Config.PLUGIN_NAME;
import static org.giuse.CodeGenerator.utils.GUI.viewManager;

public class Parser {
    private enum ClassType{CLASS,INTERFACE,ENUM}
    public static final String TAG = "Parser";
    Codebase codebase;

    VPContext context;
    private static Parser INSTANCE;
    public static String DEFAULT_PATH;

    public static Parser getInstance(VPContext context, String choosePath){
        //TODO Config.UpdateProperty("actions.generate_code.default_path",choosePath);
        String name = context.getDiagram().getName();

        DEFAULT_PATH = choosePath;

        if(INSTANCE == null)
            INSTANCE = new Parser(context, name);

        INSTANCE.codebase = new Codebase(name,new ArrayList<>(), new ArrayList<>(), DEFAULT_PATH + "\\"+name);

        return INSTANCE;
    }

    private Parser(VPContext context, String name){
        this.context = context;
        this.codebase = new Codebase(name,new ArrayList<>(), new ArrayList<>(), DEFAULT_PATH + "\\"+name);
    }

    public Codebase getCodebase(){
        return this.codebase;
    }

    private Struct parseClass(IClassUIModel iClassUIModel, String packagePath){
        String optionalPackagePath;
        IClass iClass = (IClass) iClassUIModel.getModelElement();

        //endRelationship.getMasterView().getDiagramUIModel().getName()
        
        ClassType classType;
        AtomicBoolean hasExtend = new AtomicBoolean(false);
        Struct.Builder builder;

        if(packagePath == null){
            IDiagramElement parent = iClassUIModel.getParent();
            IModelElement parentModel = parent != null ? parent.getModelElement() : null;
            Package parentPackage = null;

            if(parentModel instanceof IPackage)
                parentPackage = parsePackageBottomUp((IPackageUIModel) parent);

            optionalPackagePath = parentPackage != null ? parentPackage.getPathname() : codebase.getPathname();
        }
        else{
            optionalPackagePath = packagePath;
        }

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

        //inner classes
        for(IClass innerClass: innerClasses){
            IClassUIModel uiModel = getUIModelFromElement(innerClass);
            if(uiModel != null)
                builder.addInnerClass(parseClass(uiModel,null));
        }

        //attributes
        for (IAttribute attribute : attributes)
            if(!handleAttributeParsing(attribute,iClassUIModel,builder,classType,defaultColor))
                return null;

        //functions
        for (IOperation operation : operations)
            if(!handleFunctionParsing(operation, iClassUIModel, builder, classType, defaultColor))
                return null;

        //template
        if( (templateParameters != null) && (classType != ClassType.ENUM) )
            builder.hasTemplate(parseTemplate(templateParameters));

        //attributes from relations
        for(IRelationshipEnd relationship: relationshipsFrom)
            if(!handleEndRelationshipParsing(relationship,iClassUIModel,builder,IAssociation.DIRECTION_FROM_TO))
                return null;

        for(IRelationshipEnd relationship: relationshipsTo)
            if(!handleEndRelationshipParsing(relationship,iClassUIModel,builder,IAssociation.DIRECTION_TO_FROM))
                return null;

        //inheritance
        for(ISimpleRelationship relationship: simpleRelationshipsFrom){
            switch ( handleSimpleRelationshipParsing(relationship,iClass,builder,classType,iClassUIModel,defaultColor,hasExtend)){
                case 1:
                    return null;
                case 2:
                    continue;
                default:
            }
        }

        return builder.addConstructor().build();
    }

    private Integer handleSimpleRelationshipParsing(ISimpleRelationship relationship, IClass iClass, Struct.Builder builder, ClassType classType, IClassUIModel iClassUIModel, Color defaultColor, AtomicBoolean hasExtend){
        if (relationship instanceof IGeneralization) {
            IModelElement to = relationship.getTo();

            if(!(to instanceof IClass))
                return 2; //should step on next iteration

            if(to.hasStereotype("Interface")){
                viewManager.showMessage(iClass.getName() + " implements " + to.getName(), PLUGIN_NAME);

                if(classType == ClassType.INTERFACE)
                    ((Interface.Builder) builder).addExtends(to.getName());
                else if (classType == ClassType.CLASS)
                    ((Class.Builder) builder).addImplements(to.getName());
                else
                    ((Enum.Builder) builder).addImplements(to.getName());

                for(IOperation function: ((IClass) to).toOperationArray()){
                    Function parsedFunction = parseFunction(function);

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
                    IClassUIModel uiModel = getUIModelFromElement(to);

                    if((!hasExtend.get()) && (uiModel != null)){
                        viewManager.showMessage(iClass.getName() + " extends " + to.getName(), PLUGIN_NAME);
                        Class extended = new Class.Builder("",null,uiModel.getModelElement().getName()).build();

                        extended.setAttributes(parseAttributes((IClass) uiModel.getModelElement()));

                        ((Class.Builder) builder).setExtends(extended);
                        hasExtend.set(true);
                    }
                    else {
                        GUI.showErrorParsingMessage(iClassUIModel,TAG,relationship, iClass.getName() + " cannot extends multiple classes");
                        return 1;
                    }
                }
                else{
                    GUI.showErrorParsingMessage(iClassUIModel,TAG,relationship, iClass.getName() + " cannot extends classes");
                    return 1;
                }
            }
        }

        return 0;
    }

    private IClassUIModel getUIModelFromElement(IModelElement element){
        for(IShapeUIModel shapeUIModel: context.getDiagram().toShapeUIModelArray())
            if(shapeUIModel instanceof IClassUIModel && (shapeUIModel.getModelElement().getId().compareTo(element.getId()) == 0))
                return (IClassUIModel)shapeUIModel;

        return null;
    }

    private Boolean handleFunctionParsing(IOperation operation, IClassUIModel iClassUIModel, Struct.Builder builder, ClassType classType, Color defaultColor){
        Function parsedFunction = parseFunction(operation);

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
            String aggregationKind = relationship.getModelPropertyByName("aggregationKind").getValueAsString();
            //TODO aggregation switch here

            Attribute parsedAssociation = parseAssociation(relationship, direction);
            if(parsedAssociation != null)
                builder.addAttribute(parsedAssociation);
            else{
                iClassUIModel.setForeground(GUI.defaultError);
                endRelationship.addPropertyChangeListener(evt -> iClassUIModel.setForeground(new Color(0,0,0)));
                return false;
            }
        }

        return true;
    }

    private Boolean handleAttributeParsing(IAttribute attribute, IClassUIModel iClassUIModel, Struct.Builder builder, ClassType classType, Color defaultColor){
        Attribute parsedAttribute = parseAttribute(attribute);

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

    private Template parseTemplate(ITemplateParameter[] parameters) {
        Template.Builder builder= new Template.Builder();

        for(ITemplateParameter parameter : parameters){
            String formattedType = null;

            if(parameter.typeCount() > 0)
                 formattedType = FormatUtils.toJavaType(parameter.getTypeByIndex(0).getTypeAsString());

            if (parameter.typeCount() > 1)
                viewManager.showMessage("In " + parameter.getName() + " only first type is considered -> " +Arrays.toString(parameter.toTypeArray()), PLUGIN_NAME);

            builder.addParameter(parameter.getName(), formattedType);

            if(parameter.getDefaultValue() != null)
                viewManager.showMessage(parameter.getName() +" initial value is ignored", PLUGIN_NAME);
        }

        return builder.build();
    }

    private ArrayList<Attribute> parseAttributes(IClass aClass){
        ArrayList<Attribute> attributesList = new ArrayList<>();
        IAttribute[] attributes = aClass.toAttributeArray();

        for(IAttribute attribute: attributes)
            attributesList.add(parseAttribute(attribute));

        return attributesList;
    }

    private Attribute parseAttribute(IAttribute attribute){
        if(attribute.getTypeAsString() != null){
            viewManager.showMessage(attribute.getVisibility()+" "+attribute.getTypeAsString() +" "+ attribute.getName() + ";", PLUGIN_NAME);

            String formattedType = FormatUtils.toJavaType(attribute.getTypeAsString());

            return new Attribute(attribute.getVisibility(), formattedType, attribute.getName(), attribute.getInitialValue());
        }
        else{
            GUI.showErrorMessageDialog(viewManager.getRootFrame(), TAG, attribute.getName() +" has null type");
            return null;
        }
    }

    private Function parseFunction(IOperation function){
        if(function.getReturnTypeAsString() != null){
            viewManager.showMessage(function.getVisibility() +" "+ function.getReturnTypeAsString() +" "+ function.getName() + "()", PLUGIN_NAME);

            String returnType = function.getReturnTypeAsString();

            String formattedReturnType = returnType.compareTo("void") == 0 ? returnType : FormatUtils.toJavaType(returnType);

            Function.Builder builderFunction = new Function.Builder(function.getName(),function.getVisibility(), formattedReturnType);

            for(IParameter parameter :function.toParameterArray()){
                if(parameter.getTypeAsText() !=null){
                    String formattedType = FormatUtils.toJavaType(parameter.getTypeAsString());
                    builderFunction.addParameter(new Attribute("", formattedType,parameter.getName(), ""));
                }
                else{
                    GUI.showErrorMessageDialog(viewManager.getRootFrame(), TAG, "parameter " + parameter.getName() +" has null type");
                    return null;
                }
            }

            return builderFunction.build();
        }
        else{
            GUI.showErrorMessageDialog(viewManager.getRootFrame(), TAG, function.getName() +" has null return type");
            return null;
        }
    }

    private Attribute parseAssociation(IRelationshipEnd association, int direction){
        String toMultiplicity = ((IAssociationEnd) association.getOppositeEnd()).getMultiplicity();
        String fromMultiplicity = ((IAssociationEnd) association).getMultiplicity();
        boolean needsAssociationClass = toMultiplicity.contains("*") && fromMultiplicity.contains("*");
        String associationName = association.getEndRelationship().getName();

        if(needsAssociationClass && (associationName == null || associationName.isEmpty())){
            GUI.showErrorMessageDialog(viewManager.getRootFrame(), TAG, "many to many association needs name");
            return null;
        }

        IModelElement from, to;

        if(direction == IAssociation.DIRECTION_FROM_TO){
            from = association.getEndRelationship().getFrom();
            to = association.getEndRelationship().getTo();
        }
        else if(direction == IAssociation.DIRECTION_TO_FROM){
            from = association.getEndRelationship().getTo();
            to = association.getEndRelationship().getFrom();
        }
        else{
            throw new InvalidParameterException("direction parameter must be in range [0,1]");
        }

        if(toMultiplicity.compareTo("Unspecified") == 0){
            GUI.showErrorMessageDialog(viewManager.getRootFrame(), TAG, from.getName() + " has no multiplicity specified for " + to.getName());
            return null;
        }

        viewManager.showMessage(from.getName() + " has " + toMultiplicity + " " + to.getName(), PLUGIN_NAME);

        String attributeType;
        String initializer = null;
        String formattedType = needsAssociationClass ? FormatUtils.toJavaType(associationName) : FormatUtils.toJavaType(to.getName());
        String attributeName = to.getName();

        if(toMultiplicity.compareTo("0") == 0)
            return null;

        if(FormatUtils.isArrayList(toMultiplicity)) {
            attributeType = "ArrayList<" + formattedType + ">";
            attributeName+="s";
        }
        else if(FormatUtils.isFixedArray(toMultiplicity)){
            initializer = "new " + formattedType + "[" + FormatUtils.getFixedArrayLength(toMultiplicity) + "]";
            attributeType = formattedType+"[]";
        }
        else if(FormatUtils.isNotArray(toMultiplicity))
            attributeType = formattedType;
        else {
            GUI.showErrorMessageDialog(viewManager.getRootFrame(), TAG,toMultiplicity + " is invalid multiplicity ");
            return null;
        }

        String relationVisibility = ((IAssociation) association.getEndRelationship()).getVisibility();

        String scope = relationVisibility.compareTo("Unspecified") == 0 ? "private" : relationVisibility;

        return new Attribute(scope, attributeType, attributeName.toLowerCase(), initializer);
    }

    private void parsePackageTopDown(IPackageUIModel iPackage, Package parent){
        Package aPackage = new Package(iPackage.getModelElement().getName(), new ArrayList<>(), parent.getPathname()+"\\"+iPackage.getModelElement().getName());

        for(IShapeUIModel shapeUIModel: iPackage.toChildArray()){
            IModelElement iModelElement = shapeUIModel.getModelElement();

            if(iModelElement instanceof IClass)
                aPackage.addFile(parseClass((IClassUIModel) shapeUIModel, aPackage.getPathname()));
            else if (iModelElement instanceof IPackage)
                parsePackageTopDown((IPackageUIModel) shapeUIModel, aPackage);
        }

        codebase.addPackage(aPackage);
    }

    private Package parsePackageBottomUp(IPackageUIModel iPackage){
        IDiagramElement parent = iPackage.getParent();
        IModelElement modelParent = parent != null ? parent.getModelElement() : null;

        Package parentPackage = null;

        if(modelParent instanceof IPackage)
            parentPackage = parsePackageBottomUp((IPackageUIModel) parent);

        String optionalPackagePath = parentPackage != null ? parentPackage.getPathname() : codebase.getPathname();

        Package aPackage = new Package(iPackage.getModelElement().getName(), new ArrayList<>(), optionalPackagePath+"\\"+iPackage.getModelElement().getName());

        codebase.addPackage(aPackage);

        return aPackage;
    }

    private Package parsePackage(IPackageUIModel iPackage){
        Package aPackage = parsePackageBottomUp(iPackage);

        for(IShapeUIModel shapeUIModel: iPackage.toChildArray()){
            IModelElement iModelElement = shapeUIModel.getModelElement();

            if(iModelElement instanceof IClass)
                aPackage.addFile(parseClass((IClassUIModel) shapeUIModel, aPackage.getPathname()));
            else if (iModelElement instanceof IPackage)
                parsePackageTopDown((IPackageUIModel) shapeUIModel, aPackage);
        }

        return aPackage.getFiles().contains(null) ? null : aPackage;
    }

    public void parseDiagram(IDiagramUIModel iDiagramUIModel){
        for(IShapeUIModel shapeUIModel: iDiagramUIModel.toShapeUIModelArray()){
            IModelElement modelElement = shapeUIModel.getModelElement();

            if(modelElement instanceof IPackage){
                if(modelElement.getParent() instanceof IModel)
                    codebase.addPackage(parsePackage((IPackageUIModel) shapeUIModel));
            }
            else if(modelElement instanceof IClass){
                if(modelElement.getParent() instanceof IModel){
                    codebase.addFile(parseClass((IClassUIModel) shapeUIModel, codebase.getPathname()));
                }
            }
        }
    }

    public void parseSingleClass(IClassUIModel iClass){
        File aClass = parseClass(iClass,null);

        codebase.addFile(aClass);
    }

    public void parseSinglePackage(IPackageUIModel iPackage){
        Package aPackage = parsePackage(iPackage);

        codebase.addPackage(aPackage);
    }
}