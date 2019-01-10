package com.properties.prop.parser.util;

import org.apache.commons.lang.StringUtils;

import java.util.Arrays;

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
            return 14d;
        }else  if(x.equalsIgnoreCase(y)){
            return 13d;
        }else if(containsExact(x,y,false)){
            double posYinX=getPositionExact(x,y,false);
            return 12d + (posYinX!=-1 ? (1d/posYinX) : 0d);
        }else if(containsExactIgnoreCase(x,y,false)){
            double posYinX=getPositionExactIgnoreCase(x, y,false);
            return 11d + (posYinX!=-1 ? (1d/posYinX) : 0d);
        }else if(StringUtils.contains(x,y)){
            double posYinX=getPositionContains(x,y,false);
            return 10d + (posYinX!=-1 ? (1d/posYinX) : 0d);
        }else if(StringUtils.containsIgnoreCase(x,y)){
            double posYinX=getPositionContainsIgnoreCase(x,y,false);
            return 9d + (posYinX!=-1 ? (1d/posYinX) : 0d);
        }else if(containsExact(x,y,true)){
            double posYinX=getPositionExact(x,y,true);
            return 8d + (posYinX!=-1 ? (1d/posYinX) : 0d);
        }else if(containsExactIgnoreCase(x,y,true)){
            double posYinX=getPositionExactIgnoreCase(x, y,true);
            return 7d + (posYinX!=-1 ? (1d/posYinX) : 0d);
        }else if(StringUtils.contains(x,y)){
            double posYinX=getPositionContains(x,y,true);
            return 6d + (posYinX!=-1 ? (1d/posYinX) : 0d);
        }else if(StringUtils.containsIgnoreCase(x,y)){
            double posYinX=getPositionContainsIgnoreCase(x,y,true);
            return 5d + (posYinX!=-1 ? (1d/posYinX) : 0d);
        }else if(numberOfExactStrings(x,y)!=0){
            double numberOfContainedStrings=numberOfExactStrings(x,y);
            return 4d + numberOfContainedStrings != 1d ? (1d-1d/numberOfContainedStrings) : 0d;
        }else if(numberOfExactStringsIgnoreCase(x,y)!=0){
            double numberOfContainedStrings=numberOfExactStringsIgnoreCase(x,y);
            return 3d + numberOfContainedStrings != 1d ? (1d-1d/numberOfContainedStrings) : 0d;
        }else if(numberOfContainedStrings(x,y)!=0){
            double numberOfContainedStrings=numberOfContainedStrings(x,y);
            return 2d + numberOfContainedStrings != 1d ? (1d-1d/numberOfContainedStrings) : 0d;
        }else if(numberOfContainedStringsIgnoreCase(x,y)!=0){
            double numberOfContainedStringsIgnoreCase=numberOfContainedStringsIgnoreCase(x,y);
            return 1d+numberOfContainedStringsIgnoreCase != 1d ? (1d-1d/numberOfContainedStringsIgnoreCase) : 0d;
        }else {
            return (1d-(1d/minLevenshtein(x,y)));
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

    private static double minLevenshtein(String x,String y){
        String[] tokens=x.split(SPECIAL_CHAR_REGEX);
        double minLevenshtein=Double.MAX_VALUE;
        double position=1;
        double minPosition=2;
        for(String token : tokens){
            position++;
            double levenshtein=levensthein(token,y);
            if(levenshtein+position<minLevenshtein) {
                minPosition=position;
                minLevenshtein=levenshtein+position;
            }
        }
        return minLevenshtein;
    }

    private static int levensthein(String x, String y) {
        int[][] dp = new int[x.length() + 1][y.length() + 1];

        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                }
                else if (j == 0) {
                    dp[i][j] = i;
                }
                else {
                    dp[i][j] = min(dp[i - 1][j - 1]
                                    + costOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)),
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1);
                }
            }
        }

        return dp[x.length()][y.length()];
    }

    private static int costOfSubstitution(char a, char b) {
        return a == b ? 0 : 1;
    }

    private static int min(int... numbers) {
        return Arrays.stream(numbers)
                .min().orElse(Integer.MAX_VALUE);
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
                position += tokens[i].length();
                break;
            }
        }
        if(x.contains(y)) {
            position = getIndexOfPosition(x, y, position);
        }
        return position;
    }

    private static double getIndexOfPosition(String x, String y, double position) {
        if (position == -1d) {
            int indexOfY = x.indexOf(y);
            if (indexOfY != -1) {
                position = x.length() + indexOfY;
            }
        }
        return position;
    }
    private static double getIndexOfIgnoreCasePosition(String x, String y, double position) {
        if (position == -1d) {
            int indexOfY = StringUtils.indexOf(x.toLowerCase(),y.toLowerCase());
            if (indexOfY != -1) {
                position = x.length() + indexOfY;
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
                position += tokens[i].length();
                break;
            }
        }
        if(StringUtils.containsIgnoreCase(x,y)) {
            position = getIndexOfIgnoreCasePosition(x, y, position);
        }
        return position;
    }

    private static double numberOfExactStrings(String x,String y){
        String[] tokens=y.split(SPECIAL_CHAR_REGEX);
        double number=1d;
        double position=1d;
        for(String token : tokens){
            if(x.equals(token)){
                number+=(1d-1d/position)+1;
            }
        }
        return number;
    }
    private static double numberOfExactStringsIgnoreCase(String x,String y){
        String[] tokens=y.split(SPECIAL_CHAR_REGEX);
        double number=1d;
        double position=1d;
        for(String token : tokens){
            if(x.equalsIgnoreCase(token)){
                number+=(1d-1d/position)+1;
            }
        }
        return number;
    }
    private static double numberOfContainedStrings(String x,String y){
        String[] tokens=y.split(SPECIAL_CHAR_REGEX);
        double number=1d;
        double position=1d;
        for(String token : tokens){
            position++;
            if(x.contains(token)){
                number+=(1d-1d/position)+1;
            }
        }
        return number;
    }
    private static double numberOfContainedStringsIgnoreCase(String x,String y){
        String[] tokens=y.split(SPECIAL_CHAR_REGEX);
        double number=1d;
        double position=1d;
        for(String token : tokens){
            position++;
            if(StringUtils.containsIgnoreCase(x,token)){
                number+=(1d-1d/position)+1;
            }
        }
        return number;
    }

}
