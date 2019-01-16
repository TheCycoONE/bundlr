package com.properties.prop.parser.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
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

    public Map<String, String> getFileMap() {
        return fileMap;
    }

    public void setFileMap(Map<String, String> fileMap) {
        this.fileMap = fileMap;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bundle bundle = (Bundle) o;
        return getLastModified() == bundle.getLastModified() &&
                Objects.equals(getName(), bundle.getName()) &&
                Objects.equals(getPath(), bundle.getPath()) &&
                Objects.equals(getId(), bundle.getId()) &&
                Objects.equals(getFileMap(), bundle.getFileMap());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getPath(), getId(), getLastModified(), getFileMap());
    }
}
