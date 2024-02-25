package org.giuse.JavaGenerator.utils;

public class FormatUtils {
    public static String firstUpperFormat(String string){
        if(string == null || string.isEmpty())
            return string;

        return string.substring(0,1).toUpperCase() + string.substring(1).toLowerCase();
    }
}
