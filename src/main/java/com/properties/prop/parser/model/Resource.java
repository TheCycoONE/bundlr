package com.properties.prop.parser.model;

import javafx.beans.property.SimpleStringProperty;

import java.util.*;

public class Resource implements Comparable<Resource>{

    private final SimpleStringProperty code;

    private String id;

    private int order;

    private Map<String,SimpleStringProperty> properties;

    public Resource(String code,int order) {
        this.code = new SimpleStringProperty(code);
        properties= new LinkedHashMap<>();
        id=UUID.randomUUID().toString();
        this.order=order;
    }
    public Resource(String code,String id,int order) {
        this.code = new SimpleStringProperty(code);
        properties= new LinkedHashMap<>();
        this.id=id;
        this.order=order;
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

    public int getOrder() {
        return order;
    }

    @Override
    public int compareTo(Resource o) {
        if(this.order<o.getOrder()){
            return -1;
        }else if(this.order>o.getOrder()) {
            return 1;
        }else {
            return 0;
        }
    }
}
