package org.codegenerator.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormatUtils {
    public static final String TAG = "FormatUtils";
    private static final String ARRAYLIST_REGEX = "([0|1])\\.+\\*";
    private static final String NOT_ARRAY_REGEX = "1|(([0|1])\\.+1)";
    private static final String FIXED_ARRAY_REGEX = "([2-9])+|(([0|1])(\\.+)([2-9]+))";
    private static final String CAPS_LOCK_REGEX = "[A-Z]+";
    private static final String STRING_REGEX = "^\".*\"";

    public static final Map<String, String> listTypeJava = new HashMap<>();

    static {
        listTypeJava.put("AbstractList","java.util");
        listTypeJava.put("AbstractSequentialList","java.util");
        listTypeJava.put("ArrayList","java.util");
        listTypeJava.put("AttributeList","javax.management");
        listTypeJava.put("CopyOnWriteArrayList","java.util.concurrent");
        listTypeJava.put("LinkedList","java.util");
        listTypeJava.put("RoleList","javax.management.relation");
        listTypeJava.put("RoleUnresolvedList","javax.management.relation");
        listTypeJava.put("Stack","java.util");
        listTypeJava.put("Vector","java.util");
    }


    public static boolean isCapsLock(String string){
        return string.matches(CAPS_LOCK_REGEX);
    }

    public static String getIndentation(int indentation){
        return "\t".repeat(indentation);
    }

    public static boolean isString(String string){
        return string.matches(STRING_REGEX);
    }

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
    public static String toJavaType(String vpType){
        String firstFormat = firstUpperFormat(vpType);

        switch (firstFormat){
            case "Null":
            case "Void":
                return firstFormat.toLowerCase();
            case "Int":
                return "Integer";
            case "Char":
                return firstFormat + "acter";
            case "String":
            case "Short":
            case "Long":
            case "Float":
            case "Double":
            case "Byte":
            case "Boolean":
                return firstFormat;
            default:
                return vpType;
        }
    }
    public static String firstUpperFormat(String string){
        if(string == null || string.isEmpty())
            return string;

        return string.substring(0,1).toUpperCase() + string.substring(1).toLowerCase();
    }
}
