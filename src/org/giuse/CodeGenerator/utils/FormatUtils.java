package org.giuse.CodeGenerator.utils;

public class FormatUtils {

    public static String toJavaType(String vpPrimitive){
        String firstFormat = firstUpperFormat(vpPrimitive);

        if(firstFormat.compareTo("Void") == 0)
            return firstFormat.toLowerCase();
        else if (firstFormat.compareTo("Int") == 0)
            return "Integer";
        else
            return firstFormat;
    }
    public static String firstUpperFormat(String string){
        if(string == null || string.isEmpty())
            return string;

        return string.substring(0,1).toUpperCase() + string.substring(1).toLowerCase();
    }
}
