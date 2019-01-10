package com.properties.prop.parser.service;

import com.properties.prop.parser.model.Resource;
import com.properties.prop.parser.model.Tuple;
import javafx.collections.ObservableList;
import org.apache.commons.configuration.ConfigurationException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

public interface FileService {
    ObservableList<Resource> loadRowData(List<File> files) throws ConfigurationException, ExecutionException, InterruptedException;
    void saveOrUpdateProperty(String filePath, String key, String value,long lastModified) throws IOException, ConfigurationException;
    void updateKeyInFiles(List<Tuple> codeValues, String code, String newCode,long lastModified) throws IOException, ConfigurationException;
}
