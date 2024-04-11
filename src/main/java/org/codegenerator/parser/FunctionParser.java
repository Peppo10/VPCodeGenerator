package org.codegenerator.parser;

import com.vp.plugin.model.IClass;
import com.vp.plugin.model.IOperation;
import com.vp.plugin.model.IParameter;
import org.codegenerator.logger.Logger;
import org.codegenerator.parser.models.Attribute;
import org.codegenerator.parser.models.Function;
import org.codegenerator.parser.models.statements.Return;
import org.codegenerator.utils.CGContext;
import org.codegenerator.utils.ChooseListDialogHandler;
import org.codegenerator.utils.FormatUtils;
import static org.codegenerator.parser.Parser.getClassImport;
import static org.codegenerator.utils.GUI.viewManager;

public class FunctionParser{
    CGContext context;
    public FunctionParser(CGContext context) {
        this.context = context;
    }
    public Function parseFunction(IOperation function, IClass iClass, Boolean notify){
        boolean isInterface = iClass.hasStereotype("Interface");

        if(function.getReturnTypeAsString() != null){
            if(notify)
                Logger.queueInfoMessage("Parsing " + iClass.getName() + "-> " + function.getVisibility() +" "+ function.getReturnTypeAsString() +" "+ function.getName() + "()");

            String returnType = function.getReturnTypeAsString();

            String formattedType = FormatUtils.toJavaType(returnType);

            String visibility = function.getVisibility();

            if((function.getVisibility() != null) && notify && isInterface)
                Logger.queueWarningMessage("Parsing " + iClass.getName() + "-> " + function.getName() +" visibility is ignored");

            String formattedVisibility = isInterface? null : visibility;

            Function.Builder builderFunction = new Function.Builder(function.getName(), formattedVisibility, formattedType);

            builderFunction.addImport(getClassImport(iClass, returnType, context));

            if(formattedType.compareTo("void") != 0)
                builderFunction.addStatement(new Return("null"));

            for(IParameter parameter :function.toParameterArray()){
                if((parameter.getTypeAsText() != null) && (!parameter.getTypeAsText().isEmpty())){
                    String paramFormattedType = FormatUtils.toJavaType(parameter.getTypeAsString());
                    String multiplicity = parameter.getMultiplicity();
                    String attributeType;

                    if(FormatUtils.isArrayList(multiplicity)){
                        String typeList = "ArrayList";

                        if((!ChooseListDialogHandler.applyAlways) && (!context.getErrorFlag())){
                            ChooseListDialogHandler chooseListDialogHandler = new ChooseListDialogHandler(function.getName(), paramFormattedType);
                            viewManager.showDialog(chooseListDialogHandler);
                            typeList = chooseListDialogHandler.getChoose();
                        }

                        builderFunction.addImport(getClassImport(iClass, typeList, context));

                        attributeType = typeList + "<" + paramFormattedType + ">";
                    }
                    else if(FormatUtils.isFixedArray(multiplicity)){
                        attributeType = paramFormattedType +"[]";
                    }
                    else
                        attributeType = paramFormattedType;

                    builderFunction.addImport(getClassImport(iClass, paramFormattedType, context));

                    builderFunction.addParameter(new Attribute("", attributeType,parameter.getName(), null));
                }
                else{
                    if(notify)
                        Logger.queueErrorMessage("Parsing " + iClass.getName() + "-> " + "parameter " + parameter.getName() +" has null type");

                    return null;
                }
            }

            return builderFunction.build();
        }
        else{
            if(notify)
                Logger.queueErrorMessage("Parsing " + iClass.getName() + "-> " + function.getName() +" has null return type");

            return null;
        }
    }
}
