package org.giuse.JavaGenerator.generator;

import org.giuse.JavaGenerator.parser.models.Class;
import org.giuse.JavaGenerator.parser.models.Codebase;
import org.giuse.JavaGenerator.parser.models.Interface;
import org.giuse.JavaGenerator.parser.models.Package;
import org.giuse.JavaGenerator.utils.GUI;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.giuse.JavaGenerator.utils.Config.PLUGIN_NAME;
import static org.giuse.JavaGenerator.utils.GUI.viewManager;

public class Generator {
    public static final String TAG = "Generator";
    public static void generate(Codebase codebase){
        if(codebase.getClasses().contains(null) || codebase.getPackages().contains(null))
            return;

        if(!new File(codebase.getPathname()).mkdirs()){
            viewManager.showMessage("folder "+codebase.getName()+" was not instanced(maybe already exists)", PLUGIN_NAME);
        }

        for(Package aPackage: codebase.getPackages())
            generatePackage(aPackage);

        for(File file: codebase.getClasses()){
            try {
                generateFile(file);
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        GUI.showInformationMessageDialog(viewManager.getRootFrame(), TAG, "Code Generated Successfully");
    }

    private static void generatePackage(Package aPackage){
        if(!new File(aPackage.getPathname()).mkdirs()){
            viewManager.showMessage("folder "+aPackage.getName()+" was not instanced(maybe already exists)", PLUGIN_NAME);
        }

        for(File file: aPackage.getFiles()){
            try {
                generateFile(file);
            }catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    private static void generateFile(File file) throws IOException {
        if(file instanceof Class){
            generateClass((Class)file);
        } else if (file instanceof Interface) {
            generateInterface((Interface)file);
        }
    }

    private static void generateInterface(Interface file) throws IOException {
        if(file.createNewFile()){
            FileWriter myWriter = new FileWriter(file.getAbsolutePath());
            myWriter.write(file.generateContent());
            myWriter.close();
        }
    }

    private static void generateClass(Class file) throws IOException {
        if(file.createNewFile()){
            FileWriter myWriter = new FileWriter(file.getAbsolutePath());
            myWriter.write(file.generateContent());
            myWriter.close();
        }
    }
}
