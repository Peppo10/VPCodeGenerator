package org.giuse.codegenerator.parser.models;

import org.giuse.codegenerator.parser.models.statements.Statement;
import org.giuse.codegenerator.utils.FormatUtils;

import java.util.ArrayList;

public class Interface extends Struct implements Statement{
    private ArrayList<String> Extends;

    private Interface(String pathname,
                      String scope,
                      String name,
                      ArrayList<String> anExtends,
                      ArrayList<Attribute> attributes,
                      ArrayList<Function> functions,
                      ArrayList<Struct> innerClasses,
                      Template template,
                      String aPackage,
                      ArrayList<String> imports) {
        super(pathname, scope, name, attributes, functions, innerClasses, template ,aPackage, imports);
        Extends = anExtends;
    }

    public ArrayList<String> getExtends() {
        return Extends;
    }

    public void setExtends(ArrayList<String> anExtends) {
        Extends = anExtends;
    }

    @Override
    public String generateJava(int indentation) {
        StringBuilder interfaceContent = new StringBuilder();
        String formattedIndentation = FormatUtils.getIndentation(indentation);

        if((aPackage != null) && (!aPackage.isEmpty()) && (indentation == 0))
            interfaceContent.append(formattedIndentation).append("package ").append(aPackage).append(";\n\n");

        interfaceContent.append(formattedIndentation);

        if((super.scope != null) && (!super.scope.isEmpty()))
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
            interfaceContent.append("\n");
            interfaceContent.append(attribute.generateJava(indentation+1));
            interfaceContent.append(";\n");
        }

        for(Function function: super.functions){
            interfaceContent.append("\n");
            interfaceContent.append(function.generateJava(indentation+1));
            interfaceContent.append("\n");
        }

        for(Struct struct: innerClasses){
            interfaceContent.append("\n").append(struct.generateJava(indentation+1)).append("\n");
        }

        interfaceContent.append(formattedIndentation).append("}");

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
            return new Interface(bPathname, bScope, bName, bExtends, bAttributes, bFunctions, bInnerClasses, bTemplate, bPackage, bImports);
        }
    }
}
