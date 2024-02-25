package org.giuse.JavaGenerator.parser.models;

import java.io.File;
import java.util.ArrayList;

public class Package{
    private ArrayList<File> files;
    private String name;
    private String pathname;

    public Package(String name, ArrayList<File> files, String pathname) {
        this.name = name;
        this.files = files;
        this.pathname = pathname;
    }

    public void addFile(File file){
        this.files.add(file);
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

    public ArrayList<File> getFiles() {
        return files;
    }

    public void setFiles(ArrayList<File> files) {
        this.files = files;
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
