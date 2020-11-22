package com.limachi.ducky_library;

public class StringUtils {

    public static String camelToSnake(String str) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (i > 0) {
                char p = str.charAt(i - 1);
                if (out.length() != 0 && Character.isUpperCase(c) && Character.isLowerCase(p))
                    out.append('_');
            }
            out.append(Character.toLowerCase(c));
        }
        return out.toString();
    }

    public static String snakeToCamel(String str) {
        StringBuilder out = new StringBuilder();
        boolean capitalize = false;
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (c == '_') {
                if (out.length() != 0)
                    capitalize = true;
                continue;
            }
            if (capitalize) {
                out.append(Character.toUpperCase(c));
                capitalize = false;
            } else
                out.append(Character.toLowerCase(c));
        }
        return out.toString();
    }
}
