package org.giuse.CodeGenerator.parser.models;

import java.util.ArrayList;

public class Interface extends Struct{
    private ArrayList<String> Extends;

    public Interface(String pathname, String scope, String name, ArrayList<String> anExtends, ArrayList<Attribute> attributes, ArrayList<Function> functions, Template template) {
        super(pathname, scope, name, attributes, functions, template);
        Extends = anExtends;
    }

    public ArrayList<String> getExtends() {
        return Extends;
    }

    public void setExtends(ArrayList<String> anExtends) {
        Extends = anExtends;
    }

    public String generateContent() {
        StringBuilder interfaceContent = new StringBuilder();

        interfaceContent.append(super.scope).append(" ");

        interfaceContent.append("interface").append(" ").append(super.name);

        if(template != null)
            interfaceContent.append(template.generateContent());

        for(int i=0;i<getExtends().size();i++){
            if(i==0)
                interfaceContent.append(" extends ").append(getExtends().get(i));
            else
                interfaceContent.append(", ").append(getExtends().get(i));
        }

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
        private final ArrayList<String> bExtends;

        public Builder(String pathname, String scope, String name){
            super(pathname,scope,name);

            this.bExtends = new ArrayList<>();
        }

        public Builder addExtends(String aExtends){
            this.bExtends.add(aExtends);
            return this;
        }

        public Interface build(){
            return new Interface(bPathname, bScope, bName, bExtends, bAttributes, bFunctions, bTemplate);
        }

        public Interface buildWithConstructor(){
            super.buildWithConstructor();

            return new Interface(bPathname, bScope, bName, bExtends, bAttributes, bFunctions, bTemplate);
        }
    }
}
