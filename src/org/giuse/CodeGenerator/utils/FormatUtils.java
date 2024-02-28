package org.giuse.CodeGenerator.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.giuse.CodeGenerator.utils.GUI.viewManager;

public class FormatUtils {
    public static final String TAG = "FormatUtils";
    private static final String ARRAYLIST_REGEX = "(0|1)\\.+\\*";
    private static final String NOT_ARRAY_REGEX = "1|((0|1)\\.+1)";
    private static final String FIXED_ARRAY_REGEX = "([2-9])+|((0|1)(\\.+)([2-9]+))";

    public static String getFixedArrayLength(String array){
        Matcher matcher = Pattern.compile(FIXED_ARRAY_REGEX).matcher(array);
        String number;

        if(matcher.matches()){
            String group1 = matcher.group(1);
            String group5 = matcher.group(5);

            if(group5 != null)
                number = group5;
            else
                number = group1;

            return number;
        }

        return null;
    }

    public static boolean isArrayList(String vpType){
        return vpType.matches(ARRAYLIST_REGEX);
    }

    public static boolean isNotArray(String vpType){
        return vpType.matches(NOT_ARRAY_REGEX);
    }

    public static boolean isFixedArray(String vpType){
        return vpType.matches(FIXED_ARRAY_REGEX);
    }
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