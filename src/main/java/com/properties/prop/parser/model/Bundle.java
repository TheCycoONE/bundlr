package com.properties.prop.parser.model;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class Bundle {
    private String name;
    private String path;
    private String id;

    private long lastModified;
    private Map<String,String> fileMap;

    public Bundle(String name, String path,long lastModified) {
        this.name = name;
        this.path = path;
        id=UUID.randomUUID().toString();
        this.lastModified=lastModified;
        fileMap=new LinkedHashMap<>();
    }

    public Bundle(String name, String path,String id,long lastModified) {
        this.name = name;
        this.path = path;
        this.id = id;
        this.lastModified=lastModified;
        fileMap=new LinkedHashMap<>();
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

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public Map<String, String> getFileMap() {
        return fileMap;
    }

    public void setFileMap(Map<String, String> fileMap) {
        this.fileMap = fileMap;
    }
}
