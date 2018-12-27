package com.properties.prop.parser.service;

import com.properties.prop.parser.model.Resource;
import com.properties.prop.parser.model.Tuple;
import javafx.collections.ObservableList;
import org.apache.commons.configuration.ConfigurationException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public interface FileService {
    ObservableList<Resource> loadRowData(List<File> files) throws FileNotFoundException, ConfigurationException;
    void saveOrUpdateProperty(String filePath, String key, String value) throws IOException, ConfigurationException;
    void updateKeyInFiles(List<Tuple> codeValues, String code, String newCode) throws IOException, ConfigurationException;
    void removeFileEntries(Collection<String> filePaths, List<String> codes) throws IOException, ConfigurationException;
}
