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
        boolean isInterface = iClass.hasStereotype("Interface");
        if(isInterface && (attribute.getInitialValue() == null)){
            if(notify)
                Logger.queueErrorMessage("Parsing " + iClass.getName() + "-> " + attribute.getName() +" must have initializer on interface");

            return null;
        }

        if((attribute.getTypeAsString() != null) && (!attribute.getTypeAsString().isEmpty())) {
            if(notify)
                Logger.queueInfoMessage("Parsing " + iClass.getName() + "-> " + attribute.getVisibility()+" "+attribute.getTypeAsString() +" "+ attribute.getName() + ";");

            String formattedType = FormatUtils.toJavaType(attribute.getTypeAsString());

            String multiplicity = attribute.getMultiplicity();

            String visibility = attribute.getVisibility();

            String initializer = attribute.getInitialValue();

            String attributeType;

            if((attribute.getVisibility() != null) && notify && isInterface)
                Logger.queueWarningMessage("Parsing " + iClass.getName() + "-> " + attribute.getName() +" visibility is ignored");

            String formattedVisibility = isInterface ? null : visibility;

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
}
