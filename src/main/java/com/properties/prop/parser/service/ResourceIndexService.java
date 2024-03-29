package com.properties.prop.parser.service;

import com.properties.prop.parser.model.Resource;
import javafx.collections.ObservableList;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ResourceIndexService {
    void createStore(String storeName) throws IOException;
    void createLanguageBasedAnalyzer(String storeName, Set<String> languageFile);
    void loadStores(List<String> storeNames) throws IOException;
    void reloadDocuments(String storeName, List<Resource> resources) throws IOException;
    void updateDocument(String storeName,Resource resource) throws IOException;
    void deleteDocument(String storeName,Resource resource) throws IOException;
    void deleteDocuments(String storeName, List<Resource> resources) throws IOException;
    void deleteStore(String storeName) throws IOException;
    boolean storeExists(String storeName);
    Map<String,ObservableList<Resource>> searchIndex(String storeName, String queryString, String[] fieldsArray, String notSortedWord) throws ParseException, IOException;
    ObservableList<Resource> searchIndex(String storeName,String queryString, String field,String notSortedWord) throws ParseException, IOException;
    ObservableList<Resource> getAllResources(String storeName) throws IOException;
}
