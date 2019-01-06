package com.properties.prop.parser.util;

import org.apache.commons.lang.StringUtils;

public class StringUtil {
    public static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // These characters are part of the query syntax and must be escaped
            if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':'
                    || c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~'
                    || c == '?' || c == '|' || c == '&' || c == '/') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }
    public static double similarity(String x,String y){
        if(x.equals(y)){
            return 2d;
        }else if(StringUtils.containsIgnoreCase(x,y)){
            return 1d + 1d/Math.abs(x.length()-y.length());
        }else {
            return 1d /Math.abs(x.length()-y.length());
        }
    }

}
