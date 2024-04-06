package org.codegenerator.parser.models.statements;

import org.codegenerator.utils.FormatUtils;

public class Return implements Statement{
    String value;

    public Return(String value) {
        this.value = value;
    }

    @Override
    public String generateJava(int indentation) {
        String formattedIndentation = FormatUtils.getIndentation(indentation);
        return formattedIndentation + "return " + value + ";\n";
    }
}
