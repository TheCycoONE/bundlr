package com.properties.prop.parser.service;

import com.properties.prop.parser.factories.LocalizedAnalyzerFactory;
import com.properties.prop.parser.model.DocumentStore;
import com.properties.prop.parser.model.Resource;
import com.properties.prop.parser.util.ResourceDocumentConverter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ResourceIndexServiceImpl implements ResourceIndexService, InitializingBean {

    @Autowired
    ResourceDocumentConverter resourceDocumentConverter;
    @Autowired
    LocalizedAnalyzerFactory localizedAnalyzerFactory;
    Map<String,DocumentStore> stores;

    @Override
    public void afterPropertiesSet() {
        stores=new HashMap<>();
    }

    @Override
    public void createStore(String storeName) throws IOException {
        DocumentStore store=new DocumentStore(storeName);
        if(!stores.containsKey(storeName)) {
            stores.put(storeName, store);
        }
    }

    @Override
    public void createLanguageBasedAnalyzer(String storeName, Set<String> languageFiles) {
        if(stores.containsKey(storeName)){
            DocumentStore store=stores.get(storeName);
            if(store.getAnalyzer()==null) {
                Analyzer analyzer = localizedAnalyzerFactory.createLocalizedAnalyzer(languageFiles);
                store.setAnalyzer(analyzer);
            }
        }
    }

    @Override
    public void loadStores(List<String> storeNames) throws IOException {
        for(String storeName : storeNames){
            createStore(storeName);
        }
    }
    @Override
    public void addDocuments(String storeName, List<Resource> resources) throws IOException {
        if(stores.containsKey(storeName)){
            List<Document> documents=resourceDocumentConverter.convertAllToDocument(resources);
            DocumentStore documentStore=stores.get(storeName);
            documentStore.addDocuments(documents);
        }
    }
    @Override
    public boolean storeExists(String storeName){
        return stores.containsKey(storeName);
    }


    @Override
    public void updateDocument(String storeName, Resource resource) throws IOException {
        if(stores.containsKey(storeName)){
            Document document=resourceDocumentConverter.convertToDocument(resource);
            DocumentStore documentStore=stores.get(storeName);
            documentStore.updateDocument("code",resource.getCode(),document);
        }
    }

    @Override
    public void deleteDocument(String storeName, Resource resource) throws IOException {
        if(stores.containsKey(storeName)){
            DocumentStore documentStore=stores.get(storeName);
            documentStore.deleteDocument("id",resource.getId());
        }
    }
    @Override
    public void deleteDocuments(String storeName, List<Resource> resources) throws IOException {
        if(stores.containsKey(storeName)){
            for(Resource resource : resources){
                deleteDocument(storeName,resource);
            }
        }
    }
    @Override
    public void deleteStore(String storeName) throws IOException {
        if(stores.containsKey(storeName)){
            stores.get(storeName).clearAll();
            stores.remove(storeName);
        }
    }
    @Override
    public ObservableList<Resource> searchIndex(String storeName,String queryString, String[] fieldsArray) throws ParseException, IOException {
        if(stores.containsKey(storeName)) {
            DocumentStore documentStore = stores.get(storeName);
            List<Document> documents = documentStore.searchIndex(queryString,fieldsArray);
            List<Resource> resources = resourceDocumentConverter.convertAllToResource(documents);
            return FXCollections.observableArrayList(resources);
        }
        return FXCollections.emptyObservableList();
    }
    @Override
    public ObservableList<Resource> getAllResources(String storeName) throws IOException {
        if(stores.containsKey(storeName)) {
            DocumentStore documentStore = stores.get(storeName);
            List<Document> documents = documentStore.getAllDocuments();
            List<Resource> resources = resourceDocumentConverter.convertAllToResource(documents);
            return FXCollections.observableArrayList(resources);
        }
        return FXCollections.emptyObservableList();
    }
}
