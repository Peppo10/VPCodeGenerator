package org.giuse.codegenerator.parser.models.statements;

import org.giuse.codegenerator.utils.FormatUtils;

public class Assignment implements Statement{
    private String lValue;
    private String rValue;

    public Assignment(String lValue, String rValue) {
        this.lValue = lValue;
        this.rValue = rValue;
    }

    public String getL() {
        return lValue;
    }

    public void setL(String lValue) {
        this.lValue = lValue;
    }

    public String getR() {
        return rValue;
    }

    public void setR(String rValue) {
        this.rValue = rValue;
    }

    @Override
    public String generateJava(int indentation) {
        StringBuilder assignmentContent = new StringBuilder();
        String formattedIndentation = FormatUtils.getIndentation(indentation);

        assignmentContent.append(formattedIndentation).append(lValue).append("=").append(rValue).append(";\n");

        return assignmentContent.toString();
    }
}
