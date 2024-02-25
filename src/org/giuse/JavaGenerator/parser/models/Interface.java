package org.giuse.JavaGenerator.parser.models;

import java.util.ArrayList;

public class Interface extends Struct{
    private String Extends;

    public Interface(String pathname, String scope, String name, String anExtends, ArrayList<Attribute> attributes, ArrayList<Function> functions) {
        super(pathname, scope, name, attributes, functions);
        Extends = anExtends;
    }

    public String getExtends() {
        return Extends;
    }

    public void setExtends(String anExtends) {
        Extends = anExtends;
    }

    public String generateContent() {
        StringBuilder interfaceContent = new StringBuilder();

        interfaceContent.append(super.scope).append(" ");

        interfaceContent.append("interface").append(" ").append(super.name);

        if(getExtends() != null)
            interfaceContent.append(" extends ").append(getExtends());

        interfaceContent.append("{");

        for(Attribute attribute: super.attributes){
            interfaceContent.append("\n\t");
            interfaceContent.append(attribute.generateContent());
            interfaceContent.append(";\n");
        }

        for(Function function: super.functions){
            interfaceContent.append("\n\t");
            interfaceContent.append(function.generateContent());
            interfaceContent.append("\n");
        }

        interfaceContent.append("}");

        return interfaceContent.toString();
    }

    public static class Builder extends Struct.Builder{
        private String bExtends;

        public Builder(String pathname, String scope, String name){
            super(pathname,scope,name);
        }

        public Builder setExtends(String Extends){
            this.bExtends = Extends;
            return this;
        }

        public Interface build(){
            return new Interface(bPathname, bScope, bName, bExtends, bAttributes, bFunctions);
        }
    }
}
