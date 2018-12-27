package com.properties.prop.parser.service;

import com.properties.prop.parser.model.Bundle;
import javafx.collections.ObservableList;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;

public interface BundleService {
    void addBundle(Bundle bundle) throws IOException;
    ObservableList<Bundle> loadBundles() throws IOException;
    ObservableList<Bundle> searchBundles(String queryString) throws ParseException, IOException;
    void deleteBundle(Bundle bundle) throws IOException;
}
