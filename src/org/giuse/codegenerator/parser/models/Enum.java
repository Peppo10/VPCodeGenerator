package org.giuse.codegenerator.parser.models;

import org.giuse.codegenerator.parser.models.statements.Statement;
import org.giuse.codegenerator.utils.FormatUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Enum extends Struct implements Statement {
    Map<String, String> pairs;
    private final ArrayList<String> Implements;

    private Enum(Map<String, String> pairs, String pathname ,String scope, String name, ArrayList<String> anImplements, ArrayList<Attribute> attributes, ArrayList<Function> functions,ArrayList<Struct> innerClasses, String aPackage) {
        super(pathname, scope, name, attributes, functions, innerClasses, null, aPackage);
        this.pairs = pairs;
        Implements = anImplements;
    }

    public ArrayList<String> getImplements() {
        return Implements;
    }

    public Map<String, String> getPairs() {
        return pairs;
    }
    @Override
    public String generateJava(int indentation) {
        StringBuilder enumContent = new StringBuilder();
        String formattedIndentation = FormatUtils.getIndentation(indentation);

        if((aPackage != null) && (!aPackage.isEmpty()) && (indentation == 0))
            enumContent.append(formattedIndentation).append("package ").append(aPackage).append(";\n\n");

        enumContent.append(formattedIndentation);

        if((super.scope!= null) && (!super.scope.isEmpty()))
            enumContent.append(super.scope).append(" ");

        enumContent.append("enum").append(" ").append(super.name);

        for(int i=0;i<getImplements().size();i++){
            if(i==0)
                enumContent.append(" implements ").append(getImplements().get(i));
            else
                enumContent.append(", ").append(getImplements().get(i));
        }

        enumContent.append("{");

        Optional<String> first=pairs.keySet().stream().findFirst();

        first.ifPresent(value ->
            {
                enumContent.append("\n").append(formattedIndentation);

                pairs.forEach((s, s2) -> {
                    if (s.compareTo(value) != 0)
                        enumContent.append(", ");

                    enumContent.append(s.toUpperCase());
                });

                enumContent.append(";\n");
            }
        );

        for(Attribute attribute: super.attributes){
            enumContent.append("\n");
            enumContent.append(attribute.generateJava(indentation+1));
            enumContent.append(";\n");
        }

        for(Function function: super.functions){
            enumContent.append("\n");
            enumContent.append(function.generateJava(indentation+1));
            enumContent.append("\n");
        }

        for(Struct struct: innerClasses){
            enumContent.append("\n").append(struct.generateJava(indentation+1)).append("\n");
        }

        enumContent.append(formattedIndentation).append("}");

        return enumContent.toString();
    }

    public static class Builder extends Struct.Builder{
        private final Map<String, String> bPairs;
        private final ArrayList<String> bImplements;

        public Builder(String pathname, String scope, String name){
            super(pathname,scope,name);
            this.bImplements = new ArrayList<>();
            this.bPairs = new HashMap<>();
        }

        public Builder addPairs(String key, String value){
            this.bPairs.put(key, value);
            return this;
        }
        public Builder addImplements(String Implements){
            this.bImplements.add(Implements);
            return this;
        }

        public Enum build(){
            return new Enum(bPairs,bPathname, bScope, bName, bImplements, bAttributes, bFunctions, bInnerClasses, bPackage);
        }
    }
}