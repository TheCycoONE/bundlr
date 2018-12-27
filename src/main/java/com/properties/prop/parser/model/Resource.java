package com.properties.prop.parser.model;

import javafx.beans.property.SimpleStringProperty;

import java.util.*;

public class Resource {

    private final SimpleStringProperty code;

    private String id;

    private Map<String,SimpleStringProperty> properties;

    public Resource(String code) {
        this.code = new SimpleStringProperty(code);
        properties= new LinkedHashMap<>();
        id=UUID.randomUUID().toString();
    }
    public Resource(String code,String id) {
        this.code = new SimpleStringProperty(code);
        properties= new LinkedHashMap<>();
        this.id=id;
    }

    public String getCode() {
        return code.get();
    }

    public void setCode(String code) {
        this.code.set(code);
    }

    public String getPropertyValue(String property){
        SimpleStringProperty stringProperty= Optional.ofNullable(getProperty(property)).orElse(new SimpleStringProperty(""));
        return stringProperty.get();
    }
    public SimpleStringProperty getProperty(String property){
        return properties.get(property);
    }
    public void setProperty(String property,String value){
        properties.put(property,new SimpleStringProperty(value));
    }
    public Set<Map.Entry<String,SimpleStringProperty>> keyValuePairs(){
        return properties.entrySet();
    }
    public Map<String, SimpleStringProperty> getProperties() {
        return properties;
    }
    public String getId() {
        return id;
    }

}
