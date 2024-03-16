package org.giuse.CodeGenerator.logger;

import org.giuse.CodeGenerator.utils.Config;

import static org.giuse.CodeGenerator.utils.GUI.viewManager;

public class Logger {
    public static void showWarning(String message){
        viewManager.showMessage("WARNING: " + message, Config.PLUGIN_NAME);
    }
    public static void showInfo(String message){
        viewManager.showMessage("INFO: " + message, Config.PLUGIN_NAME);
    }
    public static void showError(String message){
        viewManager.showMessage("ERROR: " + message, Config.PLUGIN_NAME);
    }
}
