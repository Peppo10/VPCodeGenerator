package org.giuse.codegenerator.parser.models.statements;

import org.giuse.codegenerator.utils.FormatUtils;

import java.util.ArrayList;

public class Call implements Statement{
    private final String callable;
    private final ArrayList<String> parameters;

    private Call(String callable, ArrayList<String> parameters) {
        this.callable = callable;
        this.parameters = parameters;
    }

    public String getCallable() {
        return callable;
    }

    public ArrayList<String> getParameters() {
        return parameters;
    }

    @Override
    public String generateJava(int indentation) {
        StringBuilder callContent = new StringBuilder();
        String formattedIndentation = FormatUtils.getIndentation(indentation);

        callContent.append(formattedIndentation).append(callable).append("(");

        for(int i=0;i<getParameters().size();i++){
            if(i==0)
                callContent.append(getParameters().get(i));
            else
                callContent.append(", ").append(getParameters().get(i));
        }

        callContent.append(");\n");

        return callContent.toString();
    }

    public static class Builder{
        private final String bCallable;
        private final ArrayList<String> bParameters;
        public Builder(String callable){
            this.bCallable = callable;
            bParameters = new ArrayList<>();
        }
        public Builder addParameter(String parameter){
            this.bParameters.add(parameter);
            return this;
        }

        public Call build(){
            return new Call(this.bCallable, this.bParameters);
        }
    }
}
