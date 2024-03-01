package org.giuse.CodeGenerator.parser.models;

import org.giuse.CodeGenerator.parser.models.statements.Statement;
import org.giuse.CodeGenerator.utils.FormatUtils;

public class Attribute implements Statement {
    private String scope;
    private String type;
    private String name;
    private String initializer;

    public Attribute(String scope, String type, String name, String initializer) {
        this.scope = scope;
        this.type = type;
        this.name = name;
        this.initializer = initializer;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInitializer() {
        return initializer;
    }

    public void setInitializer(String initializer) {
        this.initializer = initializer;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getScope() {
        return this.scope;
    }

    @Override
    public String generateJava(int indentation) {
        StringBuilder attributeContent = new StringBuilder();
        String formattedIndentation = FormatUtils.getIndentation(indentation);

        attributeContent.append(formattedIndentation);

        if((this.scope!= null) && (!this.scope.isEmpty()))
            attributeContent.append(this.scope).append(" ");

        attributeContent.append(this.type).append(" ").append(this.name);

        if((this.initializer!= null) && (!this.initializer.isEmpty()))
            attributeContent.append("= ").append(this.initializer);

        return attributeContent.toString();
    }
}
