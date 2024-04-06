package org.codegenerator.parser;

import com.vp.plugin.model.IClass;
import com.vp.plugin.model.ITemplateParameter;
import org.codegenerator.logger.Logger;
import org.codegenerator.parser.models.Template;
import org.codegenerator.utils.FormatUtils;
import java.util.Arrays;

public class TemplateParser{
    public Template parseTemplate(ITemplateParameter[] parameters, IClass iClass) {
        Template.Builder builder= new Template.Builder();

        for(ITemplateParameter parameter : parameters){
            String formattedType = null;

            if(parameter.typeCount() > 0)
                formattedType = FormatUtils.toJavaType(parameter.getTypeByIndex(0).getTypeAsString());

            if (parameter.typeCount() > 1)
                Logger.queueWarningMessage("Parsing " + iClass.getName() + "-> " + "In " + parameter.getName() + " only first type is considered -> " + Arrays.toString(parameter.toTypeArray()));

            builder.addParameter(parameter.getName(), formattedType);

            if(parameter.getDefaultValue() != null)
                Logger.queueWarningMessage("Parsing " + iClass.getName() + "-> " + parameter.getName() +" initial value is ignored");
        }

        return builder.build();
    }
}
