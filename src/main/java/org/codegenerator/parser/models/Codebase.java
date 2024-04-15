package org.codegenerator.parser.models;

import java.io.File;
import java.util.ArrayList;

public class Codebase{
    private ArrayList<Package> packages;
    private String name;
    private ArrayList<File> files;
    private String pathname;
    public Codebase(String name,ArrayList<Package> packages, ArrayList<File> files, String pathname) {
        this.name = name;
        this.packages = packages;
        this.pathname = pathname;
        this.files = files;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void addFile(File file){
        this.files.add(file);
    }

    public void addPackage(Package aPackage){
        this.packages.add(aPackage);
    }

    public ArrayList<File> getFiles() {
        return files;
    }

    public ArrayList<File> getClasses() {
        return files;
    }

    public void setClasses(ArrayList<File> files) {
        this.files = files;
    }

    public String getPathname() {
        return pathname;
    }

    public void setPathname(String pathname) {
        this.pathname = pathname;
    }

    public ArrayList<Package> getPackages() {
        return packages;
    }

    public void setPackages(ArrayList<Package> packages) {
        this.packages = packages;
    }
}
