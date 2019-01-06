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
        if(x.equals(y)) {
            return 4d;
        }else  if(x.equalsIgnoreCase(y)){
            return 3d;
        }else if(x.contains(y)){
            return 2d + x.length()-y.length()!=0 ? 1d/Math.abs(x.length()-y.length()) : 0d;
        }else if(StringUtils.containsIgnoreCase(x,y)){
            return 1d + x.length()-y.length()!=0 ? 1d/Math.abs(x.length()-y.length()) : 0d;
        }else {
            double numberOfContainedStrings=numberOfContainedStrings(x,y);
            return numberOfContainedStrings != 0d ? 1d-1d/numberOfContainedStrings : 0d;
        }
    }
    private static double numberOfContainedStrings(String x,String y){
        String[] tokens=y.split("(\\\\s+)|(,+)|(.+)");
        double number=0d;
        for(String token : tokens){
            if(x.contains(token)){
                number++;
            }
        }
        return number;
    }

}
