package org.giuse.codegenerator.parser.models;

import org.giuse.codegenerator.parser.models.statements.Assignment;
import org.giuse.codegenerator.parser.models.statements.Statement;
import java.io.File;
import java.util.ArrayList;

public class Struct extends File implements Statement {
    protected String scope;

    protected String name;
    protected ArrayList<Attribute> attributes;
    protected ArrayList<Function> functions;
    protected Template template;
    protected ArrayList<Struct> innerClasses;
    protected String aPackage;
    protected ArrayList<String> imports;

    protected Struct(String pathname,
                     String scope,
                     String name,
                     ArrayList<Attribute> attributes,
                     ArrayList<Function> functions,
                     ArrayList<Struct> innerClasses,
                     Template template,
                     String aPackage,
                     ArrayList<String> imports) {
        super(pathname);
        this.scope = scope;
        this.name = name;
        this.attributes = attributes;
        this.functions = functions;
        this.template = template;
        this.innerClasses = innerClasses;
        this.aPackage = aPackage;
        this.imports = imports;

        for(Attribute attribute: attributes){
            for(String aImport: attribute.getImports())
                if((aImport != null) && (!imports.contains(aImport)))
                    imports.add(aImport);
        }

        for(Function function: functions){
            for(String aImport: function.getImports())
                if((aImport != null) && (!imports.contains(aImport)))
                    imports.add(aImport);
        }

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
        protected String bPackage;

        protected ArrayList<String> bImports;

        public Builder(String pathname, String scope, String name){
            this.bPathname = pathname;
            this.bScope = scope;
            this.bName = name;
            this.bAttributes = new ArrayList<>();
            this.bFunctions = new ArrayList<>();
            this.bInnerClasses = new ArrayList<>();
            this.bImports = new ArrayList<>();
        }

        public Builder addAttribute(Attribute attribute){
            this.bAttributes.add(attribute);
            return this;
        }

        public Builder setPackage(String aPackage){
            this.bPackage = aPackage;
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

        public Builder addImport(String aImport){
            if((aImport != null) && (!this.bImports.contains(aImport)))
                this.bImports.add(aImport);

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
            return new Struct(bPathname, bScope, bName, bAttributes, bFunctions, bInnerClasses, bTemplate, bPackage, bImports);
        }
    }
}
