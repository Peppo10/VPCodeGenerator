package org.giuse.JavaGenerator.parser;

import com.vp.plugin.diagram.IDiagramElement;
import com.vp.plugin.diagram.IDiagramUIModel;
import com.vp.plugin.model.*;
import org.giuse.JavaGenerator.parser.models.*;
import org.giuse.JavaGenerator.parser.models.Class;
import org.giuse.JavaGenerator.parser.models.Package;
import org.giuse.JavaGenerator.utils.Config;
import org.giuse.JavaGenerator.utils.FormatUtils;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import static org.giuse.JavaGenerator.utils.Config.PLUGIN_NAME;
import static org.giuse.JavaGenerator.utils.GUI.viewManager;

public class Parser {
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

    private Struct parseClass(IClass iClass, String packagePath){
        String optionalPackagePath;

        if(packagePath == null){
            IModelElement parent = iClass.getParent();
            Package parentPackage = null;

            if( parent instanceof IPackage)
                parentPackage = parsePackageBottomUp((IPackage) parent);

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

        for (IAttribute attribute : attributes) {
            Attribute parsedAttribute = parseAttribute(attribute);

            if(parsedAttribute != null)
                builder.addAttribute(parsedAttribute);
            else
                return null;
        }

        for (IOperation operation : operations) {
            Function parsedFunction = parseFunction(operation);

            if(parsedFunction != null){
                if(builder instanceof Interface.Builder)
                    parsedFunction.setVirtual(true);

                builder.addFunction(parsedFunction);
            }
            else
                return null;
        }

        for(IRelationshipEnd relationship: relationshipsFrom){
            if(relationship.getEndRelationship() instanceof IAssociation){
                Attribute parsedAssociation = parseAssociation(relationship, IAssociation.DIRECTION_FROM_TO);
                if(parsedAssociation != null)
                    builder.addAttribute(parsedAssociation);
                else
                    return null;
            }
        }

        for(IRelationshipEnd relationship: relationshipsTo){
            if(relationship.getEndRelationship() instanceof IAssociation){
                Attribute parsedAssociation = parseAssociation(relationship, IAssociation.DIRECTION_TO_FROM);
                if(parsedAssociation != null)
                    builder.addAttribute(parsedAssociation);
                else
                    return null;
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
            viewManager.showMessage(attribute.getName() +" has null type", PLUGIN_NAME);
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
                    viewManager.showMessage("parameter " + parameter.getName() +" has null type", PLUGIN_NAME);
                    return null;
                }
            }

            return builderFunction.build();
        }
        else{
            viewManager.showMessage(function.getName() +" has null return type", PLUGIN_NAME);
            return null;
        }
    }

    private Attribute parseAssociation(IRelationshipEnd association, int direction){
        String toMultiplicity = ((IAssociationEnd) association.getOppositeEnd()).getMultiplicity();
        String fromMultiplicity = ((IAssociationEnd) association).getMultiplicity();
        boolean needsAssociationClass = toMultiplicity.contains("*") && fromMultiplicity.contains("*");
        String associationName = association.getEndRelationship().getName();

        if(needsAssociationClass && (associationName == null || associationName.isEmpty())){
            viewManager.showMessage("many to many association needs name -> " + associationName, PLUGIN_NAME);
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
            viewManager.showMessage(from.getName() + " has no multiplicity specified for " + to.getName(), PLUGIN_NAME);
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

    private Interface parseInterface(IClass iInterface, String packagePath){
        return (Interface) parseClass(iInterface,packagePath);
    }

    private void parsePackageTopDown(IPackage iPackage, Package parent){
        Package aPackage = new Package(iPackage.getName(), new ArrayList<>(), parent.getPathname()+"\\"+iPackage.getName());

        for(IModelElement iModelElement: iPackage.toChildArray()){
            if(iModelElement instanceof IClass){
                if(iModelElement.hasStereotype("Interface"))
                    aPackage.addFile(parseInterface((IClass) iModelElement, aPackage.getPathname()));
                else
                    aPackage.addFile(parseClass((IClass) iModelElement, aPackage.getPathname()));
            } else if (iModelElement instanceof IPackage) {
                parsePackageTopDown((IPackage) iModelElement, aPackage);
            }
        }

        codebase.addPackage(aPackage);
    }

    private Package parsePackageBottomUp(IPackage iPackage){
        IModelElement parent =iPackage.getParent();

        Package parentPackage = null;

        if(parent instanceof IPackage)
            parentPackage = parsePackageBottomUp((IPackage) parent);

        String optionalPackagePath = parentPackage != null ? parentPackage.getPathname() : codebase.getPathname();

        Package aPackage = new Package(iPackage.getName(), new ArrayList<>(), optionalPackagePath+"\\"+iPackage.getName());

        codebase.addPackage(aPackage);

        return aPackage;
    }

    public void parseDiagram(IDiagramUIModel iDiagramUIModel){
        for(IDiagramElement iPackage: iDiagramUIModel.toDiagramElementArray()){
            if(iPackage.getModelElement() instanceof IPackage)
                parsePackage((IPackage) iPackage.getModelElement());
        }
    }

    public void parseSingleClass(IClass iClass){
        Class aClass = (Class) parseClass(iClass,null);

        codebase.addFile(aClass);
    }

    public void parseSingleInterface(IClass iInterface){
        Interface anInterface = (Interface) parseClass(iInterface,null);

        codebase.addFile(anInterface);
    }

    public Package parsePackage(IPackage iPackage){
        Package aPackage = parsePackageBottomUp(iPackage);

        for(IModelElement iModelElement: iPackage.toChildArray()){
            if(iModelElement instanceof IClass){
                if(iModelElement.hasStereotype("Interface"))
                    aPackage.addFile(parseInterface((IClass) iModelElement, aPackage.getPathname()));
                else
                    aPackage.addFile(parseClass((IClass) iModelElement, aPackage.getPathname()));
            } else if (iModelElement instanceof IPackage) {
                parsePackageTopDown((IPackage) iModelElement, aPackage);
            }
        }

        codebase.addPackage(aPackage);

        return aPackage;
    }
}
