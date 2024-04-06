package org.codegenerator.parser;

import com.vp.plugin.model.*;
import org.codegenerator.logger.Logger;
import org.codegenerator.parser.models.Attribute;
import org.codegenerator.utils.CGContext;
import org.codegenerator.utils.ChooseListDialogHandler;
import org.codegenerator.utils.FormatUtils;

import java.util.ArrayList;
import static org.codegenerator.parser.Parser.*;
import static org.codegenerator.utils.GUI.viewManager;

public class AttributeParser{
    CGContext context;
    public AttributeParser(CGContext context) {
        this.context = context;
    }
    public Attribute parseAttribute(IAttribute attribute, IClass iClass, Boolean notify){
        if(iClass.hasStereotype("Interface") && (attribute.getInitialValue() == null)){
            if(notify)
                Logger.queueErrorMessage("Parsing " + iClass.getName() + "-> " + attribute.getName() +" must have initializer on interface");

            return null;
        }

        if(attribute.getTypeAsString() != null) {
            if(notify)
                Logger.queueInfoMessage("Parsing " + iClass.getName() + "-> " + attribute.getVisibility()+" "+attribute.getTypeAsString() +" "+ attribute.getName() + ";");

            String formattedType = FormatUtils.toJavaType(attribute.getTypeAsString());

            String multiplicity = attribute.getMultiplicity();

            String visibility = attribute.getVisibility();

            String initializer = attribute.getInitialValue();

            String attributeType;

            if((attribute.getVisibility() != null) && notify)
                Logger.queueWarningMessage("Parsing " + iClass.getName() + "-> " + attribute.getName() +" visibility is ignored");

            String formattedVisibility = iClass.hasStereotype("Interface") ? null : visibility;

            ArrayList<String> imports = new ArrayList<>();

            if(FormatUtils.isArrayList(multiplicity)){
                String typeList = "ArrayList";

                if((!ChooseListDialogHandler.applyAlways) && (!context.getErrorFlag())){
                    ChooseListDialogHandler chooseListDialogHandler = new ChooseListDialogHandler(iClass.getName(), formattedType);
                    viewManager.showDialog(chooseListDialogHandler);
                    typeList = chooseListDialogHandler.getChoose();
                }

                imports.add(getClassImport(iClass, typeList, context));

                attributeType = typeList + "<" + formattedType + ">";
            }
            else if(FormatUtils.isFixedArray(multiplicity)){
                initializer = "new " + formattedType + "[" + FormatUtils.getFixedArrayLength(multiplicity) + "]";
                attributeType = formattedType+"[]";
            }
            else
                attributeType = formattedType;

            imports.add(getClassImport(iClass, formattedType, context));

            return new Attribute(formattedVisibility, attributeType, attribute.getName(), initializer, imports);
        }
        else{
            if(notify)
                Logger.queueErrorMessage("Parsing " + iClass.getName() + "-> " + attribute.getName() +" has null type");

            return null;
        }
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
}
