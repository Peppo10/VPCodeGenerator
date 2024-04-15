package org.codegenerator.parser.models;

public class Package{
    private String name;
    private String pathname;

    public Package(String name, String pathname) {
        this.name = name;
        this.pathname = pathname;
    }

    public String getPathname() {
        return pathname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPathname(String pathname) {
        this.pathname = pathname;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null)
            return false;

        if (getClass() != obj.getClass())
            return false;

        return this.getPathname().compareTo(((Package) obj).getPathname()) == 0;
    }
}
