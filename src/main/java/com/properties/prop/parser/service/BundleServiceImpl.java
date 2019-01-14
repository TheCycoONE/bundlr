package com.properties.prop.parser.service;

import com.properties.prop.parser.factories.AnalyzerFactory;
import com.properties.prop.parser.model.Bundle;
import com.properties.prop.parser.model.DocumentStore;
import com.properties.prop.parser.util.BundleDocumentConverter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class BundleServiceImpl implements BundleService, InitializingBean {

    private DocumentStore bundleStore;

    @Autowired
    BundleDocumentConverter bundleDocumentConverter;

    @Autowired
    AnalyzerFactory analyzerFactory;

    @Override
    public void afterPropertiesSet() throws Exception {
        bundleStore=new DocumentStore("bundleStore");
        bundleStore.setAnalyzer(analyzerFactory.createAnalyzer());
    }

    public synchronized void addBundle(Bundle bundle) throws IOException {
        Document document=bundleDocumentConverter.convertToDocument(bundle);
        bundleStore.addDocument(document);
    }
    public synchronized void addBundles(List<Bundle> bundles) throws IOException {
        List<Document> documents=bundleDocumentConverter.convertAllToDocument(bundles);
        bundleStore.addDocuments(documents);
    }

    public synchronized void updateBundle(Bundle bundle) throws IOException {
        Document document=bundleDocumentConverter.convertToDocument(bundle);
        bundleStore.updateDocument("id",bundle.getId(),document);
    }

    public synchronized void deleteBundle(Bundle bundle) throws IOException {
        bundleStore.deleteDocument("id",bundle.getId());
    }

    public ObservableList<Bundle> loadBundles() throws IOException {
        List<Document> documents=bundleStore.getAllDocuments();
        return FXCollections.synchronizedObservableList(FXCollections.observableArrayList(bundleDocumentConverter.convertAllToBundle(documents)));
    }
    public ObservableList<Bundle> searchBundles(String queryString) throws ParseException, IOException {
        List<Document> documents = bundleStore.searchIndex(queryString,"name");
        List<Bundle> bundles = bundleDocumentConverter.convertAllToBundle(documents);
        return FXCollections.observableArrayList(bundles);
    }

}
