package org.codegenerator.parser.models;

import java.util.*;

public class Template {
    private final Map<String, String> typeName;

    public Template(Map<String,String> typeName) {
        this.typeName = typeName;
    }

    public Map<String, String> getTypeName() {
        return typeName;
    }

    public String generateContent() {
        StringBuilder templateContent = new StringBuilder();

        templateContent.append("<");
        Optional<String> first=typeName.keySet().stream().findFirst();

        first.ifPresent(value -> typeName.forEach((s, s2) -> {
            if (s.compareTo(value) != 0)
                templateContent.append(", ");

            templateContent.append(s);

            if ((s2 != null) && (!s2.isEmpty()))
                templateContent.append(" extends ").append(s2);
        }));

        templateContent.append(">");

        return templateContent.toString();
    }

    public static class Builder{
        private final Map<String, String> bTypeName;

        public Builder(){
            this.bTypeName = new HashMap<>();
        }

        public Builder addParameter(String name, String type){
            this.bTypeName.put(name,type);
            return this;
        }

        public Template build(){
            return new Template(this.bTypeName);
        }
    }
}
