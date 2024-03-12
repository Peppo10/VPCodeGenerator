package org.giuse.CodeGenerator.parser.models;

import org.giuse.CodeGenerator.parser.models.statements.Assignment;
import org.giuse.CodeGenerator.parser.models.statements.Statement;

import java.io.File;
import java.util.ArrayList;

public class Struct extends File implements Statement {
    protected String scope;

    protected String name;
    protected ArrayList<Attribute> attributes;
    protected ArrayList<Function> functions;
    protected Template template;
    protected ArrayList<Struct> innerClasses;

    public Struct(){
        super("");
        throw new UnsupportedOperationException("Struct cannot be instanced!");
    }

    protected Struct(String pathname, String scope, String name, ArrayList<Attribute> attributes, ArrayList<Function> functions,ArrayList<Struct> innerClasses, Template template) {
        super(pathname);
        this.scope = scope;
        this.name = name;
        this.attributes = attributes;
        this.functions = functions;
        this.template = template;
        this.innerClasses = innerClasses;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public void setAttributes(ArrayList<Attribute> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String generateJava(int indentation) {
        //not implemented in java
        return null;
    }

    public static abstract class Builder{
        protected final String bScope;
        protected final String bName;
        protected final String bPathname;
        protected final ArrayList<Attribute> bAttributes;
        protected final ArrayList<Function> bFunctions;
        protected Template bTemplate;
        protected ArrayList<Struct> bInnerClasses;

        public Builder(String pathname, String scope, String name){
            this.bPathname = pathname;
            this.bScope = scope;
            this.bName = name;
            this.bAttributes = new ArrayList<>();
            this.bFunctions = new ArrayList<>();
            this.bInnerClasses = new ArrayList<>();
        }

        public Builder addAttribute(Attribute attribute){
            this.bAttributes.add(attribute);
            return this;
        }

        public Builder addInnerClass(Struct innerClass){
            this.bInnerClasses.add(innerClass);
            return this;
        }

        public Builder addFunction(Function function){
            this.bFunctions.add(function);
            return this;
        }

        public Builder setTemplate(Template template){
            this.bTemplate = template;
            return this;
        }

        public Builder addConstructor(){
            Function.Builder builder = new Function.Builder(this.bName,"public","");

            for(Attribute attribute: this.bAttributes){
                builder.addParameter(new Attribute("",attribute.getType(),attribute.getName(),""));
                builder.addStatement(new Assignment("this."+attribute.getName(),attribute.getName()));
            }

            this.addFunction(builder.build());

            return this;
        }

        public Struct build(){
            return new Struct(bPathname, bScope, bName, bAttributes, bFunctions, bInnerClasses, bTemplate);
        }
    }
}
