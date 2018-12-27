package com.properties.prop.parser.model;

public class Tuple {
    private String key;
    private String value;

    public Tuple(){

    }

    public Tuple(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

}
