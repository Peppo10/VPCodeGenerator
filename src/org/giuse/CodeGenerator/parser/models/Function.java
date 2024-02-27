package org.giuse.CodeGenerator.parser.models;

import java.util.ArrayList;

public class Function{
    private String scope;
    private String returnType;
    private String name;
    private ArrayList<Attribute> parameters;

    private Boolean isVirtual;

    public Function(String scope, String returnType, String name, ArrayList<Attribute> parameters, Boolean isVirtual) {
        this.scope = scope;
        this.returnType = returnType;
        this.name = name;
        this.parameters = parameters;
        this.isVirtual = isVirtual;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Attribute> getParameters() {
        return parameters;
    }

    public void setParameters(ArrayList<Attribute> parameters) {
        this.parameters = parameters;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getScope() {
        return this.scope;
    }

    public Boolean getVirtual() {
        return isVirtual;
    }

    public void setVirtual(Boolean virtual) {
        isVirtual = virtual;
    }

    public String generateContent() {
        StringBuilder functionContent = new StringBuilder();

        functionContent.append(this.scope).append(" ").append(this.returnType).append(" ").append(this.name).append("(");

        for(int i=0;i<getParameters().size();i++){
            if(i==0)
                functionContent.append(getParameters().get(i).generateContent());
            else
                functionContent.append(", ").append(getParameters().get(i).generateContent());
        }

        functionContent.append(")");

        if(getVirtual())
            functionContent.append(";");
        else
            functionContent.append("{}");

        return functionContent.toString();
    }

    public static class Builder{
        private final String bScope;
        private final String bName;
        private final String bReturnType;

        private Boolean bIsVirtual;
        private final ArrayList<Attribute> bParameters;

        public Builder(String name, String scope, String returnType){
            this.bScope = scope;
            this.bName = name;
            this.bReturnType = returnType;
            this.bParameters = new ArrayList<>();
            this.bIsVirtual = false;
        }

        public Builder isVirtual(){
            this.bIsVirtual = true;
            return this;
        }

        public Builder addParameter(Attribute parameter){
            this.bParameters.add(parameter);
            return this;
        }

        public Function build(){
            return new Function(bScope, bReturnType, bName, bParameters, bIsVirtual);
        }
    }
}
