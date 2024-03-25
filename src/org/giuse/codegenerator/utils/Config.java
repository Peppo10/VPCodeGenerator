package org.giuse.codegenerator.utils;

import com.vp.plugin.ApplicationManager;
import java.io.*;
import java.util.Properties;

public class Config {
    private static final String ROOT_PATH = ApplicationManager.instance().getPluginInfo("codeGenerator").getPluginDir().getAbsolutePath();
    private static final String CONFIG_PATH = String.join(File.separator, ROOT_PATH, "plugin.properties");
    private static final String ASSETS_PATH = String.join(File.separator, ROOT_PATH, "assets");
    public static final String IMAGES_PATH = String.join(File.separator, ASSETS_PATH, "images");
    public static final String ICONS_PATH = String.join(File.separator, ASSETS_PATH, "icons");
    private static final Properties pluginProperties = new Properties();

    static {
        try {
            pluginProperties.load(new FileInputStream(CONFIG_PATH));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void UpdateProperty(String key, String value){
        try (FileOutputStream out = new FileOutputStream(CONFIG_PATH)){
            pluginProperties.setProperty(key, value);
            pluginProperties.store(out, null);
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }
    public static final String PLUGIN_ID = pluginProperties.getProperty("plugin.id");
    public static final String PLUGIN_NAME = pluginProperties.getProperty("plugin.name");
    public static final String PLUGIN_DESCRIPTION = pluginProperties.getProperty("plugin.description");
    public static final String PLUGIN_VERSION = pluginProperties.getProperty("plugin.version");
    public static final String PLUGIN_PROVIDER = pluginProperties.getProperty("plugin.provider");
    public static final String PLUGIN_CONTACT = pluginProperties.getProperty("plugin.contact");
    public static final String PLUGIN_LICENSE_FILE = pluginProperties.getProperty("plugin.license");
    public static final String PLUGIN_TEAM = pluginProperties.getProperty("plugin.team");
    public static final String GENERATE_CODE_ACTION = pluginProperties.getProperty("actions.generate_code.label");
    public static final String GENERATE_CODE_DEFAULT_PATH = pluginProperties.getProperty("actions.generate_code.default_path");
}
