package org.giuse.CodeGenerator.parser.models;

import java.io.File;
import java.util.ArrayList;

public class Struct extends File {
    protected String scope;
    protected String name;
    protected ArrayList<Attribute> attributes;
    protected ArrayList<Function> functions;

    public Struct(){
        super("");
        throw new UnsupportedOperationException("Struct cannot be instanced!");
    }

    protected Struct(String pathname, String scope, String name, ArrayList<Attribute> attributes, ArrayList<Function> functions) {
        super(pathname);
        this.scope = scope;
        this.name = name;
        this.attributes = attributes;
        this.functions = functions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(ArrayList<Attribute> attributes) {
        this.attributes = attributes;
    }

    public ArrayList<Function> getFunctions() {
        return functions;
    }

    public void setFunctions(ArrayList<Function> functions) {
        this.functions = functions;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getScope() {
        return this.scope;
    }

    public static abstract class Builder{
        protected final String bScope;
        protected final String bName;
        protected final String bPathname;
        protected final ArrayList<Attribute> bAttributes;
        protected final ArrayList<Function> bFunctions;

        public Builder(String pathname, String scope, String name){
            this.bPathname = pathname;
            this.bScope = scope;
            this.bName = name;
            this.bAttributes = new ArrayList<>();
            this.bFunctions = new ArrayList<>();
        }

        public Builder addAttribute(Attribute attribute){
            this.bAttributes.add(attribute);
            return this;
        }

        public Builder addFunction(Function function){
            this.bFunctions.add(function);
            return this;
        }

        public Struct build(){
            return new Struct(bPathname, bScope, bName, bAttributes, bFunctions);
        }
    }
}
