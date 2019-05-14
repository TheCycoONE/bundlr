package com.properties.prop.parser.service;

import com.properties.prop.parser.model.Resource;
import com.properties.prop.parser.util.LayoutWrapper;
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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class FileServiceImpl implements FileService {

    public ObservableList<Resource> loadRowData(List<File> files) throws ConfigurationException, ExecutionException, InterruptedException, IOException {
        Set<String> keys=new LinkedHashSet<>();
        Map<String,Resource> resourceMap=new LinkedHashMap<>();
        int k=0;
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
                            resource=new Resource(key,k);
                            k++;
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
    /*public static String doMagic(String s) {
        if(s==null) return null;

        char[] data=s.toCharArray();
        int lpos, upos; *//* Latin position, Unicode position. *//*
        char t; *//* For building chars. *//*
        int count; *//* Number of following bytes in character code. *//*
        boolean changed=false; *//* If the string was modified. *//*

        for(lpos=upos=0; lpos<data.length; lpos++) {
            if(data[lpos]<0x80) {
                *//* Yay, plain ascii *//*
                data[upos]=data[lpos];
                upos++;
            } else if(data[lpos]>0xFF) {
                *//* Not Latin1 String! *//*
                return s;
            } else if(data[lpos]>0xEF) {
                *//* Our chars are 16 bit, so these won't work anyways. *//*
                return s;
            } else {
                t=data[lpos];
                if((t|0xE0)==t) count=2; *//* Two additional bytes. *//*
                else if((t|0xC0)==t) count=1; *//* Just one additional. *//*
                else return s; *//* Not valid UTF-8. *//*

                if(lpos+count>=data.length)
                    return s; *//* We're missing bytes. *//*

                t=(char)(t&(((1<<(6-count))-1)));

                for(int i=1; i<=count; i++) {
                    if(data[lpos+i]>0xBF)
                        return s; *//* Invalid follow-up char. *//*
                    t=(char)((t<<6)|(data[lpos+i]&0x3F));
                }

                lpos+=count;
                data[upos]=t;
                upos++;
                changed=true;
            }
        }
        if(changed)
            return new String(data,0,upos);
        else return s;
    }*/

    @Override
    public void saveOrUpdateProperty(String filePath, String key, String value) throws IOException, ConfigurationException {
        File file = new File(filePath);
        PropertiesConfiguration config = new PropertiesConfiguration();
        PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout(config);
        InputStreamReader reader=new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
        layout.load(reader);
        FileWriter writer=new FileWriter(filePath, StandardCharsets.UTF_8,false);
        LayoutWrapper wrapper=new LayoutWrapper(writer,layout);
        wrapper.save(key,value);
    }

}
