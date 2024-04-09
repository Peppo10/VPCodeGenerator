package org.codegenerator.parser;

import com.vp.plugin.diagram.ICompartmentColorModel;
import com.vp.plugin.diagram.shape.IClassUIModel;
import com.vp.plugin.model.*;
import org.codegenerator.logger.Logger;
import org.codegenerator.parser.models.*;
import org.codegenerator.parser.models.Class;
import org.codegenerator.parser.models.Enum;
import org.codegenerator.utils.CGContext;
import org.codegenerator.utils.ChooseListDialogHandler;
import org.codegenerator.utils.FormatUtils;
import org.codegenerator.utils.GUI;
import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.codegenerator.parser.Parser.*;
import static org.codegenerator.parser.Parser.getClassImport;
import static org.codegenerator.utils.GUI.viewManager;

public class RelationParser {
    private final FunctionParser functionParser;
    private final AttributeParser attributeParser;
    CGContext context;
    public RelationParser(CGContext context) {
        this.functionParser = new FunctionParser(context);
        this.attributeParser = new AttributeParser(context);
        this.context = context;
    }

    /**
     * parse simple relationship
     *
     * @return 0-Parsed successfully 1-Error 2-Parsed skipped
     */
    public Integer handleSimpleRelationshipParsing(ISimpleRelationship relationship, IClass iClass, Struct.Builder builder, ClassParser.ClassType classType, IClassUIModel iClassUIModel, Color defaultColor, AtomicBoolean hasExtend, CGContext context){
        if (relationship instanceof IGeneralization) {
            IModelElement to = relationship.getTo();

            if (!(to instanceof IClass) || !(to.getAddress().startsWith(context.getPath())))
                return 2; //should step on next iteration

            builder.addImport(getClassImport(iClass, to.getName(), context));

            if(to.hasStereotype("Interface")){
                Logger.queueInfoMessage("Parsing " + iClass.getName() + "-> " + " implements " + to.getName());

                if(classType == ClassParser.ClassType.INTERFACE)
                    ((Interface.Builder) builder).addExtends(to.getName());
                else if (classType == ClassParser.ClassType.CLASS)
                    ((Class.Builder) builder).addImplements(to.getName());
                else
                    ((Enum.Builder) builder).addImplements(to.getName());

                for(IOperation function: ((IClass) to).toOperationArray()){
                    Function parsedFunction = functionParser.parseFunction(function, iClass, false);

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
                if(classType == ClassParser.ClassType.CLASS){
                    if((!hasExtend.get())){
                        Logger.queueInfoMessage("Parsing " + iClass.getName() + "-> " + " extends " + to.getName());

                        Class extended = new Class.Builder("",null, to.getName()).build();

                        extended.setAttributes(parseExtendAttributes((IClass) to));

                        ((Class.Builder) builder).setExtends(extended);
                        hasExtend.set(true);
                    }
                    else {
                        GUI.showErrorParsingMessage(iClassUIModel,relationship, iClass.getName() + " cannot extends multiple classes");

                        return 1;
                    }
                }
                else{
                    GUI.showErrorParsingMessage(iClassUIModel,relationship, iClass.getName() + " cannot extends classes");

                    return 1;
                }
            }
        }

        return 0;
    }

    public Attribute parseAssociation(IRelationshipEnd association, IClass iClass, int direction){
        String toMultiplicity = ((IAssociationEnd) association.getOppositeEnd()).getMultiplicity();
        String fromMultiplicity = ((IAssociationEnd) association).getMultiplicity();
        boolean needsAssociationClass = toMultiplicity.contains("*") && fromMultiplicity.contains("*");
        String associationName = association.getEndRelationship().getName();

        if(needsAssociationClass && (associationName == null || associationName.isEmpty())){
            Logger.queueErrorMessage("Parsing " + iClass.getName() + "-> " + "many to many association needs name");
            return null;
        }

        IModelElement from, to;
        from = getFromModelElement(association, direction);
        to = getToModelElement(association, direction);

        String aggregationKind = association.getModelPropertyByName("aggregationKind").getValueAsString();

        if(aggregationKind.compareTo("Shared") == 0)
            Logger.queueWarningMessage("Parsing " + iClass.getName() + "-> Aggregation relation between " + from.getName() + " and " + to.getName() + " is treated as association");
        else if(aggregationKind.compareTo("Composited") == 0)
            Logger.queueWarningMessage("Parsing " + iClass.getName() + "-> Composition relation between " + from.getName() + " and " + to.getName() + " is treated as association");

        if(toMultiplicity.compareTo("Unspecified") == 0){
            Logger.queueErrorMessage("Parsing " + iClass.getName() + "-> has no multiplicity specified for " + to.getName());
            return null;
        }

        Logger.queueInfoMessage("Parsing " + iClass.getName() + "-> " + "has " + toMultiplicity + " " + to.getName());

        String attributeType;
        String initializer = null;
        String formattedType = needsAssociationClass ? FormatUtils.toJavaType(associationName) : FormatUtils.toJavaType(to.getName());
        String attributeName = to.getName();

        ArrayList<String> imports = new ArrayList<>();

        if(toMultiplicity.compareTo("0") == 0)
            return  new Attribute(null, null, null, null, new ArrayList<>());

        if(FormatUtils.isArrayList(toMultiplicity)){
            String typeList = "ArrayList";

            if((!ChooseListDialogHandler.applyAlways) && (!context.getErrorFlag())){
                ChooseListDialogHandler chooseListDialogHandler = new ChooseListDialogHandler(from.getName(), to.getName());
                viewManager.showDialog(chooseListDialogHandler);
                typeList = chooseListDialogHandler.getChoose();
            }

            imports.add(getClassImport(iClass, typeList, context));

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
            Logger.queueErrorMessage("Parsing " + iClass.getName() + "-> " + toMultiplicity + " is invalid multiplicity ");

            return null;
        }

        imports.add(getClassImport(iClass, formattedType, context));

        String relationVisibility = ((IAssociation) association.getEndRelationship()).getVisibility();

        String scope;

        if(from.hasStereotype("Interface")){
            Logger.queueWarningMessage("Parsing " + iClass.getName() + "-> " + to.getName() + " visibility is ignored");
            scope = null;

            if(initializer == null)
                initializer = "null";

        }
        else{
            scope = (relationVisibility.compareTo("Unspecified") == 0) ? "private" : relationVisibility;
        }

        return new Attribute(scope, attributeType, attributeName.toLowerCase(), initializer, imports);
    }

    private ArrayList<Attribute> parseExtendAttributes(IClass aClass){
        ArrayList<Attribute> attributesList = new ArrayList<>();
        IAttribute[] attributes = aClass.toAttributeArray();

        for(IAttribute attribute: attributes)
            attributesList.add(attributeParser.parseAttribute(attribute, aClass, false));

        return attributesList;
    }
}
