package com.properties.prop.parser.util;

import org.apache.commons.lang.StringUtils;

public class StringUtil {

    public static final String SPECIAL_CHAR_REGEX = "( +)|(,+)|(\\.+)|(:+)|(;+)|('+)|(\"+)";

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
    public static double positionSimilarity(String x,String y){
        if(x.equals(y)) {
            return 17d;
        }else  if(x.equalsIgnoreCase(y)){
            return 16d;
        }else if(containsExact(x,y,false)){
            double posYinX=getPositionExact(x,y,false);
            return 15d + (posYinX!=-1 ? (1d/posYinX) : 0d);
        }else if(containsExactIgnoreCase(x,y,false)){
            double posYinX=getPositionExactIgnoreCase(x, y,false);
            return 14d + (posYinX!=-1 ? (1d/posYinX) : 0d);
        }else if(StringUtils.contains(x,y)){
            double posYinX=getPositionContains(x,y,false);
            return 13d + (posYinX!=-1 ? (1d/posYinX) : 0d);
        }else if(StringUtils.containsIgnoreCase(x,y)){
            double posYinX=getPositionContainsIgnoreCase(x,y,false);
            return 12d + (posYinX!=-1 ? (1d/posYinX) : 0d);
        }else if(numberOfExactStrings(x,y,false)!=0){
            double numberOfContainedStrings=numberOfContainedStrings(x,y,false);
            return 11d + numberOfContainedStrings != 1d ? (1d-1d/numberOfContainedStrings) : 0d;
        }else if(numberOfExactStringsIgnoreCase(x,y,false)!=0){
            double numberOfContainedStrings=numberOfContainedStrings(x,y,false);
            return 10d + numberOfContainedStrings != 1d ? (1d-1d/numberOfContainedStrings) : 0d;
        }else if(numberOfContainedStrings(x,y,false)!=0){
            double numberOfContainedStrings=numberOfContainedStrings(x,y,false);
            return 9d + numberOfContainedStrings != 1d ? (1d-1d/numberOfContainedStrings) : 0d;
        }else if(numberOfContainedStringsIgnoreCase(x,y,false)!=0){
            double numberOfContainedStringsIgnoreCase=numberOfContainedStringsIgnoreCase(x,y,false);
            return 8d + numberOfContainedStringsIgnoreCase != 1d ? (1d-1d/numberOfContainedStringsIgnoreCase) : 0d;
        }else if(containsExact(x,y,true)){
            double posYinX=getPositionExact(x,y,true);
            return 7d + (posYinX!=-1 ? (1d/posYinX) : 0d);
        }else if(containsExactIgnoreCase(x,y,true)){
            double posYinX=getPositionExactIgnoreCase(x, y,true);
            return 6d + (posYinX!=-1 ? (1d/posYinX) : 0d);
        }else if(StringUtils.contains(x,y)){
            double posYinX=getPositionContains(x,y,true);
            return 5d + (posYinX!=-1 ? (1d/posYinX) : 0d);
        }else if(StringUtils.containsIgnoreCase(x,y)){
            double posYinX=getPositionContainsIgnoreCase(x,y,true);
            return 4d + (posYinX!=-1 ? (1d/posYinX) : 0d);
        }else if(numberOfExactStrings(x,y,true)!=0){
            double numberOfContainedStrings=numberOfContainedStrings(x,y,true);
            return 3d + numberOfContainedStrings != 1d ? (1d-1d/numberOfContainedStrings) : 0d;
        }else if(numberOfExactStringsIgnoreCase(x,y,true)!=0){
            double numberOfContainedStrings=numberOfContainedStrings(x,y,true);
            return 2d + numberOfContainedStrings != 1d ? (1d-1d/numberOfContainedStrings) : 0d;
        }else if(numberOfContainedStrings(x,y,true)!=0){
            double numberOfContainedStrings=numberOfContainedStrings(x,y,true);
            return 1d + numberOfContainedStrings != 1d ? (1d-1d/numberOfContainedStrings) : 0d;
        }else{
            double numberOfContainedStringsIgnoreCase=numberOfContainedStringsIgnoreCase(x,y,true);
            return numberOfContainedStringsIgnoreCase != 1d ? (1d-1d/numberOfContainedStringsIgnoreCase) : 0d;
        }
    }
    private static boolean containsExact(String x,String y,boolean escapeY){
        String[] tokens=x.split(SPECIAL_CHAR_REGEX);
        if(escapeY){
            y=y.replaceAll(SPECIAL_CHAR_REGEX,"");
        }
        boolean contains=false;
        for(String token : tokens){
            if(token.equals(y)) {
                contains = true;
                break;
            }
        }
        return contains;
    }
    private static boolean containsExactIgnoreCase(String x,String y,boolean escapeY){
        String[] tokens=x.split(SPECIAL_CHAR_REGEX);
        if(escapeY){
            y=y.replaceAll(SPECIAL_CHAR_REGEX,"");
        }
        boolean contains=false;
        for(String token : tokens){
            if(token.equalsIgnoreCase(y)) {
                contains = true;
                break;
            }
        }
        return contains;
    }
    private static double getPositionExact(String x, String y,boolean escapeY){
        String[] tokens=x.split(SPECIAL_CHAR_REGEX);
        if(escapeY){
            y=y.replaceAll(SPECIAL_CHAR_REGEX,"");
        }
        double position=-1d;
        for(int i=0;i<tokens.length;i++){
            if(tokens[i].equals(y)) {
                position = i+2;
                break;
            }
        }
        return position;
    }
    private static double getPositionExactIgnoreCase(String x, String y,boolean escapeY){
        String[] tokens=x.split(SPECIAL_CHAR_REGEX);
        if(escapeY){
            y=y.replaceAll(SPECIAL_CHAR_REGEX,"");
        }
        double position=-1d;
        for(int i=0;i<tokens.length;i++){
            if(tokens[i].equalsIgnoreCase(y)) {
                position = i+2;
                break;
            }
        }
        return position;
    }
    private static double getPositionContains(String x, String y,boolean escapeY){
        String[] tokens=x.split(SPECIAL_CHAR_REGEX);
        if(escapeY){
            y=y.replaceAll(SPECIAL_CHAR_REGEX,"");
        }
        double position=-1d;
        for(int i=0;i<tokens.length;i++){
            if(tokens[i].contains(y)) {
                position = i+2;
                break;
            }
        }
        return position;
    }
    private static double getPositionContainsIgnoreCase(String x, String y,boolean escapeY){
        String[] tokens=x.split(SPECIAL_CHAR_REGEX);
        if(escapeY){
            y=y.replaceAll(SPECIAL_CHAR_REGEX,"");
        }
        double position=-1d;
        for(int i=0;i<tokens.length;i++){
            if(StringUtil.containsExactIgnoreCase(tokens[i],y,escapeY)) {
                position = i+2;
                break;
            }
        }
        return position;
    }
    private static double numberOfExactStrings(String x,String y,boolean escapeY){
        String[] tokens=x.split(SPECIAL_CHAR_REGEX);
        if(escapeY){
            y=y.replaceAll(SPECIAL_CHAR_REGEX,"");
        }
        double number=1d;
        for(String token : tokens){
            if(token.equals(y)){
                number++;
            }
        }
        return number;
    }
    private static double numberOfExactStringsIgnoreCase(String x,String y,boolean escapeY){
        String[] tokens=x.split(SPECIAL_CHAR_REGEX);
        if(escapeY){
            y=y.replaceAll(SPECIAL_CHAR_REGEX,"");
        }
        double number=1d;
        for(String token : tokens){
            if(token.equalsIgnoreCase(y)){
                number++;
            }
        }
        return number;
    }
    private static double numberOfContainedStrings(String x,String y,boolean escapeY){
        String[] tokens=x.split(SPECIAL_CHAR_REGEX);
        if(escapeY){
            y=y.replaceAll(SPECIAL_CHAR_REGEX,"");
        }
        double number=1d;
        for(String token : tokens){
            if(token.contains(y)){
                number++;
            }
        }
        return number;
    }
    private static double numberOfContainedStringsIgnoreCase(String x,String y,boolean escapeY){
        String[] tokens=x.split(SPECIAL_CHAR_REGEX);
        if(escapeY){
            y=y.replaceAll(SPECIAL_CHAR_REGEX,"");
        }
        double number=1d;
        for(String token : tokens){
            if(StringUtils.containsIgnoreCase(token,y)){
                number++;
            }
        }
        return number;
    }

}
