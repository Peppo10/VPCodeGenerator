package org.codegenerator.generator;

import org.codegenerator.logger.Logger;
import org.codegenerator.logger.Logger.Message;
import org.codegenerator.parser.models.Class;
import org.codegenerator.parser.models.Codebase;
import org.codegenerator.parser.models.Enum;
import org.codegenerator.parser.models.Interface;
import org.codegenerator.parser.models.Package;
import org.codegenerator.utils.GUI;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import static org.codegenerator.utils.GUI.viewManager;

public class Generator {
    public static final String TAG = "Generator";
    public static void generate(Codebase codebase){
        if(codebase.getClasses().contains(null) || codebase.getPackages().contains(null))
            return;

        if(!new File(codebase.getPathname()).mkdirs())
            Logger.showMessage(new Message(Message.MessageType.WARNING,"folder "+codebase.getName()+" was not instanced(maybe already exists)"));

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
        if(!new File(aPackage.getPathname()).mkdirs())
            Logger.showMessage(new Message(Message.MessageType.WARNING,"folder "+aPackage.getName()+" was not instanced(maybe already exists)"));

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
        } else if (file instanceof Enum) {
            generateEnum((Enum)file);
        }
    }

    private static void generateEnum(Enum file) throws IOException {
        file.createNewFile();
        FileWriter myWriter = new FileWriter(file.getAbsolutePath());
        myWriter.write(file.generateJava(0));
        myWriter.close();
    }

    private static void generateInterface(Interface file) throws IOException {
        file.createNewFile();
        FileWriter myWriter = new FileWriter(file.getAbsolutePath());
        myWriter.write(file.generateJava(0));
        myWriter.close();
    }

    private static void generateClass(Class file) throws IOException {
        file.createNewFile();
        FileWriter myWriter = new FileWriter(file.getAbsolutePath());
        myWriter.write(file.generateJava(0));
        myWriter.close();
    }
}
