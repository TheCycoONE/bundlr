package com.properties.prop.parser.util;

import com.properties.prop.parser.model.Bundle;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class BundleDocumentConverter {
    public Document convertToDocument(Bundle bundle){
        Document document=new Document();
        document.add(new TextField("name",bundle.getName(), Field.Store.YES));
        document.add(new NumericDocValuesField("size-name",bundle.getName().length()));
        document.add(new TextField("path",bundle.getPath(), Field.Store.YES));
        document.add(new StringField("lastModified",String.valueOf(bundle.getLastModified()), Field.Store.YES));
        Set<Map.Entry<String,String>> fileEntries=bundle.getFileMap().entrySet();
        for(Map.Entry<String,String> entry : fileEntries){
            document.add(new TextField(entry.getKey(),entry.getValue(), Field.Store.YES));
        }
        FieldType fieldType=new FieldType();
        fieldType.setStored(true);
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        Field idField=new Field("id",bundle.getId(),fieldType);
        document.add(idField);
        return document;
    }
    public Bundle convertToBundle(Document document){
        Bundle bundle=new Bundle(document.get("name"),document.get("path"),document.get("id"),Long.valueOf(document.get("lastModified")));
        List<String> forbbidenNames=List.of("name","path","id","lastModified");
        List<IndexableField> remainingFields=document.getFields().stream().filter(Predicate.not(indexableField -> forbbidenNames.contains(indexableField.name()))).collect(Collectors.toList());
        Map<String,String> fileMap=bundle.getFileMap();
        for(IndexableField field : remainingFields){
            fileMap.put(field.name(),field.stringValue());
        }
        return bundle;
    }
    public List<Document> convertAllToDocument(List<Bundle> resources){
        return resources.stream().map(this::convertToDocument).collect(Collectors.toList());
    }
    public List<Bundle> convertAllToBundle(List<Document> documents){
        return documents.stream().map(this::convertToBundle).collect(Collectors.toList());
    }
}
