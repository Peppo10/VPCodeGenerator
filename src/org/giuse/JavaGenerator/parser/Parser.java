package org.giuse.JavaGenerator.parser;
import com.vp.plugin.diagram.*;
import com.vp.plugin.diagram.shape.IClassUIModel;
import com.vp.plugin.diagram.shape.IPackageUIModel;
import com.vp.plugin.model.*;
import org.giuse.JavaGenerator.parser.models.*;
import org.giuse.JavaGenerator.parser.models.Class;
import org.giuse.JavaGenerator.parser.models.Package;
import org.giuse.JavaGenerator.utils.FormatUtils;
import org.giuse.JavaGenerator.utils.GUI;
import java.awt.*;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import static org.giuse.JavaGenerator.utils.Config.PLUGIN_NAME;
import static org.giuse.JavaGenerator.utils.GUI.viewManager;

public class Parser {
    public static final String TAG = "Parser";
    Codebase codebase;
    private static Parser INSTANCE;

    public static String DEFAULT_PATH;

    public static Parser getInstance(String name, String choosePath){
        //TODO Config.UpdateProperty("actions.generate_code.default_path",choosePath);

        DEFAULT_PATH = choosePath;

        if(INSTANCE == null)
            INSTANCE = new Parser(name);

        INSTANCE.codebase = new Codebase(name,new ArrayList<>(), new ArrayList<>(), DEFAULT_PATH + "\\"+name);

        return INSTANCE;
    }

    private Parser(String name){
        this.codebase = new Codebase(name,new ArrayList<>(), new ArrayList<>(), DEFAULT_PATH + "\\"+name);
    }

    public Codebase getCodebase(){
        return this.codebase;
    }

    private Struct parseClass(IClassUIModel iClassUIModel, String packagePath){
        String optionalPackagePath;
        IClass iClass = (IClass) iClassUIModel.getModelElement();

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

        Struct.Builder builder = iClass.hasStereotype("Interface") ?
                                new Interface.Builder(optionalPackagePath + "\\" + iClass.getName() + ".java", "public", iClass.getName()) :
                                new Class.Builder(optionalPackagePath + "\\" + iClass.getName() + ".java", "public", iClass.getName());

        if(builder instanceof Class.Builder)
            if(iClass.isAbstract())
                ((Class.Builder)builder).isAbstract();

        IAttribute[] attributes = iClass.toAttributeArray();
        IOperation[] operations = iClass.toOperationArray();
        IRelationshipEnd[] relationshipsFrom = iClass.toFromRelationshipEndArray();
        IRelationshipEnd[] relationshipsTo = iClass.toToRelationshipEndArray();
        ITemplateParameter[] templateParameters = iClass.toTemplateParameterArray();
        Color defaultColor = iClassUIModel.getFillColor().getColor1();
        Color defaultError = new Color(255,0,0,255);

        for (IAttribute attribute : attributes) {
            Attribute parsedAttribute = parseAttribute(attribute);

            if(parsedAttribute != null)
                builder.addAttribute(parsedAttribute);
            else{
                ICompartmentColorModel attributeColor = iClassUIModel.getCompartmentColorModel(attribute,true);
                attributeColor.setBackground(defaultError);
                attribute.addPropertyChangeListener(evt -> attributeColor.setBackground(defaultColor));
                return null;
            }
        }

        for (IOperation operation : operations) {
            Function parsedFunction = parseFunction(operation);

            if(parsedFunction != null){
                if(builder instanceof Interface.Builder)
                    parsedFunction.setVirtual(true);

                builder.addFunction(parsedFunction);
            }
            else{
                ICompartmentColorModel attributeColor = iClassUIModel.getCompartmentColorModel(operation,true);
                attributeColor.setBackground(defaultError);

                for(IParameter parameter :operation.toParameterArray())
                    parameter.addPropertyChangeListener(evt -> attributeColor.setBackground(defaultColor));

                operation.addPropertyChangeListener(evt -> attributeColor.setBackground(defaultColor));
                return null;
            }
        }

        for(IRelationshipEnd relationship: relationshipsFrom){
            if(relationship.getEndRelationship() instanceof IAssociation){
                Attribute parsedAssociation = parseAssociation(relationship, IAssociation.DIRECTION_FROM_TO);
                if(parsedAssociation != null)
                    builder.addAttribute(parsedAssociation);
                else{
                    iClassUIModel.setForeground(defaultError);
                    relationship.getEndRelationship().addPropertyChangeListener(evt -> iClassUIModel.setForeground(new Color(0,0,0)));
                    return null;
                }
            }
        }

        for(IRelationshipEnd relationship: relationshipsTo){
            if(relationship.getEndRelationship() instanceof IAssociation){
                Attribute parsedAssociation = parseAssociation(relationship, IAssociation.DIRECTION_TO_FROM);
                if(parsedAssociation != null)
                    builder.addAttribute(parsedAssociation);
                else{
                    iClassUIModel.setForeground(defaultError);
                    relationship.getEndRelationship().addPropertyChangeListener(evt -> iClassUIModel.setForeground(new Color(0,0,0)));
                    return null;
                }
            }
        }

        return builder.build();
    }

    private Attribute parseAttribute(IAttribute attribute){
        if(attribute.getTypeAsString() != null){
            viewManager.showMessage(attribute.getVisibility()+" "+attribute.getTypeAsString() +" "+ attribute.getName() + ";", PLUGIN_NAME);

            String formattedType = FormatUtils.firstUpperFormat(attribute.getTypeAsString());

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

            String formattedReturnType = returnType.compareTo("void") == 0 ? returnType : FormatUtils.firstUpperFormat(returnType);

            Function.Builder builderFunction = new Function.Builder(function.getName(),function.getVisibility(), formattedReturnType);

            for(IParameter parameter :function.toParameterArray()){
                if(parameter.getTypeAsText() !=null){
                    String formattedType = FormatUtils.firstUpperFormat(parameter.getTypeAsString());
                    builderFunction.addParameter(new Attribute("", formattedType,parameter.getName(), null));
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
        String formattedType = needsAssociationClass ? FormatUtils.firstUpperFormat(associationName) : FormatUtils.firstUpperFormat(to.getName());
        String attributeName = to.getName();

        if(toMultiplicity.compareTo("0") == 0)
            return null;

        if(toMultiplicity.contains("*")) {
            attributeType = "ArrayList<" + formattedType + ">";
            attributeName+="s";
        }
        else
            attributeType = formattedType;

        String relationVisibility = ((IAssociation) association.getEndRelationship()).getVisibility();

        String scope = relationVisibility.compareTo("Unspecified") == 0 ? "private" : relationVisibility;

        return new Attribute(scope, attributeType, attributeName.toLowerCase(), null);
    }

    private Interface parseInterface(IClassUIModel iInterface, String packagePath){
        return (Interface) parseClass(iInterface,packagePath);
    }

    private void parsePackageTopDown(IPackageUIModel iPackage, Package parent){
        Package aPackage = new Package(iPackage.getModelElement().getName(), new ArrayList<>(), parent.getPathname()+"\\"+iPackage.getModelElement().getName());

        for(IShapeUIModel shapeUIModel: iPackage.toChildArray()){
            IModelElement iModelElement = shapeUIModel.getModelElement();
            if(iModelElement instanceof IClass){
                if(iModelElement.hasStereotype("Interface"))
                    aPackage.addFile(parseInterface((IClassUIModel) shapeUIModel, aPackage.getPathname()));
                else
                    aPackage.addFile(parseClass((IClassUIModel) shapeUIModel, aPackage.getPathname()));
            } else if (iModelElement instanceof IPackage) {
                parsePackageTopDown((IPackageUIModel) shapeUIModel, aPackage);
            }
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
            if(iModelElement instanceof IClass){
                if(iModelElement.hasStereotype("Interface"))
                    aPackage.addFile(parseInterface((IClassUIModel) shapeUIModel, aPackage.getPathname()));
                else
                    aPackage.addFile(parseClass((IClassUIModel) shapeUIModel, aPackage.getPathname()));
            } else if (iModelElement instanceof IPackage) {
                parsePackageTopDown((IPackageUIModel) shapeUIModel, aPackage);
            }
        }

        return aPackage.getFiles().contains(null) ? null : aPackage;
    }

    public void parseDiagram(IDiagramUIModel iDiagramUIModel){
        for(IShapeUIModel shapeUIModel: iDiagramUIModel.toShapeUIModelArray()){
            IModelElement modelElement = shapeUIModel.getModelElement();

            if(modelElement instanceof IPackage)
                codebase.addPackage(parsePackage((IPackageUIModel) shapeUIModel));
            else if(shapeUIModel.getModelElement() instanceof IClass){
                if(modelElement.hasStereotype("Interface"))
                    codebase.addFile(parseInterface((IClassUIModel) shapeUIModel, codebase.getPathname()));
                else
                    codebase.addFile(parseClass((IClassUIModel) shapeUIModel, codebase.getPathname()));
            }
        }
    }

    public void parseSingleClass(IClassUIModel iClass){
        Class aClass = (Class) parseClass(iClass,null);

        codebase.addFile(aClass);
    }

    public void parseSingleInterface(IClassUIModel iInterface){
        Interface anInterface = (Interface) parseClass(iInterface,null);

        codebase.addFile(anInterface);
    }

    public void parseSinglePackage(IPackageUIModel iPackage){
        Package aPackage = parsePackage(iPackage);

        codebase.addPackage(aPackage);
    }
}