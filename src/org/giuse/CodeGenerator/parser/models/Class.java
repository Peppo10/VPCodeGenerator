package org.giuse.CodeGenerator.parser.models;

import org.giuse.CodeGenerator.parser.models.statements.Assignment;
import org.giuse.CodeGenerator.parser.models.statements.Call;
import org.giuse.CodeGenerator.parser.models.statements.Statement;
import org.giuse.CodeGenerator.utils.FormatUtils;

import java.util.ArrayList;

public class Class extends Struct implements Statement {
    private Struct Extends;
    private ArrayList<String> Implements;
    private Boolean isAbstract;

    private Class(String pathname, Boolean isAbstract ,String scope, String name, Struct anExtends, ArrayList<String> anImplements, ArrayList<Attribute> attributes, ArrayList<Function> functions,ArrayList<Struct> innerClasses, Template template) {
        super(pathname, scope, name, attributes, functions, innerClasses, template);
        Extends = anExtends;
        Implements = anImplements;
        this.isAbstract = isAbstract;
    }

    public Boolean getAbstract() {
        return isAbstract;
    }

    public void setAbstract(Boolean anAbstract) {
        isAbstract = anAbstract;
    }

    public Struct getExtends() {
        return Extends;
    }

    public void setExtends(Struct anExtends) {
        Extends = anExtends;
    }

    public ArrayList<String> getImplements() {
        return Implements;
    }

    public void setImplements(ArrayList<String> anImplements) {
        Implements = anImplements;
    }

    @Override
    public String generateJava(int indentation) {
        StringBuilder classContent = new StringBuilder();
        String formattedIndentation = FormatUtils.getIndentation(indentation);

        classContent.append(formattedIndentation);

        if((super.scope!= null) && (!super.scope.isEmpty()))
            classContent.append(super.scope).append(" ");

        if(getAbstract())
            classContent.append("abstract").append(" ");

        classContent.append("class").append(" ").append(super.name);

        if(template != null)
            classContent.append(template.generateContent());

        if(getExtends() != null)
            classContent.append(" extends ").append(getExtends().getName());

        for(int i=0;i<getImplements().size();i++){
            if(i==0)
                classContent.append(" implements ").append(getImplements().get(i));
            else
                classContent.append(", ").append(getImplements().get(i));
        }

        classContent.append("{");

        for(Attribute attribute: super.attributes){
            classContent.append("\n");
            classContent.append(attribute.generateJava(indentation+1));
            classContent.append(";\n");
        }

        for(Function function: super.functions){
            classContent.append("\n");
            classContent.append(function.generateJava(indentation+1));
            classContent.append("\n");
        }

        for(Struct struct: innerClasses){
            classContent.append("\n").append(struct.generateJava(indentation+1)).append("\n");
        }

        classContent.append(formattedIndentation).append("}");

        return classContent.toString();
    }

    public static class Builder extends Struct.Builder{
        private Boolean bIsAbstract;
        private Struct bExtends;
        private final ArrayList<String> bImplements;

        public Builder(String pathname, String scope, String name){
            super(pathname,scope,name);
            this.bImplements = new ArrayList<>();
            this.bIsAbstract = false;
        }

        public Builder isAbstract(){
            this.bIsAbstract = true;
            return this;
        }

        public Builder setExtends(Struct Extends){
            this.bExtends = Extends;
            return this;
        }
        public Builder addImplements(String Implements){
            this.bImplements.add(Implements);
            return this;
        }

        public Builder addConstructor(){
            Function.Builder builder = new Function.Builder(this.bName,"public","");

            if(bExtends != null) {
                Call.Builder callBuilder = new Call.Builder("super");
                for (Attribute attribute : bExtends.attributes){
                    builder.addParameter(new Attribute("",attribute.getType(),attribute.getName(),""));
                    callBuilder.addParameter(attribute.getName());
                }

                builder.addStatement(callBuilder.build());
            }

            for(Attribute attribute: this.bAttributes){
                builder.addParameter(new Attribute("",attribute.getType(),attribute.getName(),""));
                builder.addStatement(new Assignment("this."+attribute.getName(),attribute.getName()));
            }

            this.addFunction(builder.build());

            return this;
        }

        public Class build(){
            return new Class(bPathname, bIsAbstract, bScope, bName, bExtends, bImplements, bAttributes, bFunctions, bInnerClasses, bTemplate);
        }
    }
}
