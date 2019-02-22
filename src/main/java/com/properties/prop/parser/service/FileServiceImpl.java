package com.properties.prop.parser.service;

import com.properties.prop.parser.model.Resource;
import com.properties.prop.parser.model.Tuple;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.PropertiesConfigurationLayout;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class FileServiceImpl implements FileService {

    public ObservableList<Resource> loadRowData(List<File> files) throws ConfigurationException, ExecutionException, InterruptedException, IOException {
        Set<String> keys=new LinkedHashSet<>();
        Map<String,Resource> resourceMap=new LinkedHashMap<>();
        List<PropertiesConfiguration> propertiesConfigurations=new LinkedList<>();
        for (File file : files) {
            PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration(file);
            Iterator<String> keyIterator=propertiesConfiguration.getKeys();
            while (keyIterator.hasNext()){
                String key=keyIterator.next();
                ByteArrayInputStream byteArrayInputStream=new ByteArrayInputStream(key.getBytes(Charset.forName("ISO-8859-1")));
                BOMInputStream bomInputStream=new BOMInputStream(byteArrayInputStream);
                if(bomInputStream.hasBOM()){
                    bomInputStream.skip(bomInputStream.getBOM().length());
                }
                String noBOMKey=IOUtils.toString(bomInputStream,Charset.forName("UTF-8"));
                if(!noBOMKey.startsWith("#")&&!noBOMKey.equals("")) {
                    keys.add(noBOMKey);
                }
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
                            resource.setProperty(fileName,new String(value.getBytes(Charset.forName("ISO-8859-1")), Charset.forName("UTF-8")));
                        }else {
                            resource.setProperty(fileName,"");
                        }
                }
        }
        if(!resourceMap.values().isEmpty()) {
            return FXCollections.observableArrayList(resourceMap.values());
        }else {
            return FXCollections.emptyObservableList();
        }
    }

    @Override
    public void saveOrUpdateProperty(String filePath, String key, String value) throws IOException, ConfigurationException {
        File file = new File(filePath);
        PropertiesConfiguration config = new PropertiesConfiguration();
        PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout(config);
        layout.load(new InputStreamReader(new FileInputStream(file),Charset.forName("ISO-8859-1")));
        config.setProperty(key,new String(value.getBytes(Charset.forName("UTF-8")), Charset.forName("ISO-8859-1")));
        layout.save(new FileWriter(filePath, Charset.forName("ISO-8859-1"),false));
    }

    @Override
    public void updateKeyInFiles(List<Tuple> codeValues, String code, String newCode) throws IOException, ConfigurationException {
        for(Tuple fileKeyValuePair : codeValues){
            updateKeyValue(code,newCode,fileKeyValuePair);
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
        layout.load(new InputStreamReader(new FileInputStream(file),Charset.forName("ISO-8859-1")));
        if(fileKeyValuePair.getValue()!=null&&!fileKeyValuePair.getValue().equals("")) {
            layout.getConfiguration().setProperty(code,null);
            layout.getConfiguration().setProperty(newCode,new String(fileKeyValuePair.getValue().getBytes(Charset.forName("UTF-8")), Charset.forName("ISO-8859-1")) );
        }
        layout.save(new FileWriter(fileKeyValuePair.getKey(), Charset.forName("ISO-8859-1"),false));
        System.out.println();
    }
}
