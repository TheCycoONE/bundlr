package com.properties.prop.parser.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Bundle {
    private String name;
    private String path;
    private String id;
    private Map<String,String> fileMap;

    public Bundle(String name, String path) {
        this.name = name;
        this.path = path;
        id=UUID.randomUUID().toString();
        fileMap=new HashMap<>();
    }

    public Bundle(String name, String path,String id) {
        this.name = name;
        this.path = path;
        this.id = id;
        fileMap=new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getId() {
        return id;
    }

    public Map<String, String> getFileMap() {
        return fileMap;
    }

    public void setFileMap(Map<String, String> fileMap) {
        this.fileMap = fileMap;
    }
}
