package com.properties.prop.parser.service;

import com.properties.prop.parser.model.Resource;
import com.properties.prop.parser.model.Tuple;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.PropertiesConfigurationLayout;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Service
public class FileServiceImpl implements FileService {

    public ObservableList<Resource> loadRowData(List<File> files) throws FileNotFoundException, ConfigurationException {
        Set<String> keys=new LinkedHashSet<>();
        Map<String,Resource> resourceMap=new LinkedHashMap<>();
        List<PropertiesConfiguration> propertiesConfigurations=new LinkedList<>();
        for (File file : files) {
            PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration(file);
            Iterator<String> keyIterator=propertiesConfiguration.getKeys();
            while (keyIterator.hasNext()){
                keys.add(keyIterator.next());
            }
            propertiesConfigurations.add(propertiesConfiguration);
        }
        for(PropertiesConfiguration propertiesConfiguration : propertiesConfigurations){
            for(String key : keys){
                String value=propertiesConfiguration.getString(key);
                Resource resource;
                if(resourceMap.containsKey(key)){
                    resource=resourceMap.get(key);
                }else {
                    resource=new Resource(key);
                    resourceMap.put(key, resource);
                }
                String fileName= FilenameUtils.getBaseName(propertiesConfiguration.getFileName());
                if(value!=null){
                    resource.setProperty(fileName,value);
                }else {
                    resource.setProperty(fileName,"");
                }
            }
        }
        return FXCollections.observableArrayList(resourceMap.values());
    }

    @Override
    public void saveOrUpdateProperty(String filePath, String key, String value) throws IOException, ConfigurationException {
        File file = new File(filePath);
        PropertiesConfiguration config = new PropertiesConfiguration();
        PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout(config);
        layout.load(new InputStreamReader(new FileInputStream(file)));
        config.setProperty(key, value);
        layout.save(new FileWriter(filePath, false));
    }

    @Override
    public void updateKeyInFiles(List<Tuple> codeValues, String code, String newCode) throws IOException, ConfigurationException {
        for(Tuple fileKeyValuePair : codeValues){
            updateKeyValue(code,newCode,fileKeyValuePair);
        }
    }

    private void removeProperty(String filePath,String code) throws IOException, ConfigurationException {
        File file = new File(filePath);
        PropertiesConfigurationLayout layout = getPropertiesConfiguration(file);
        layout.getConfiguration().clearProperty(code);
        layout.save(new FileWriter(filePath, false));
    }
    private void removeProperties(Collection<String> filePaths,String code) throws IOException, ConfigurationException {
        for(String filePath : filePaths){
            removeProperty(filePath,code);
        }
    }

    public void removeFileEntries(Collection<String> filePaths,List<String> codes) throws IOException, ConfigurationException {
        for(String code : codes){
            removeProperties(filePaths,code);
        }
    }

    private PropertiesConfigurationLayout getPropertiesConfiguration(File file) throws ConfigurationException, FileNotFoundException {
        PropertiesConfiguration config = new PropertiesConfiguration();
        PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout(config);
        layout.load(new InputStreamReader(new FileInputStream(file)));
        return layout;
    }

    private void updateKeyValue(String code,String newCode,Tuple fileKeyValuePair) throws IOException, ConfigurationException {
        File file = new File(fileKeyValuePair.getKey());
        PropertiesConfigurationLayout layout = getPropertiesConfiguration(file);
        if(fileKeyValuePair.getValue()!=null&&!fileKeyValuePair.getValue().equals("")) {
            layout.getConfiguration().setProperty(newCode, fileKeyValuePair.getValue());
        }
        layout.save(new FileWriter(fileKeyValuePair.getKey(), false));
    }
}
