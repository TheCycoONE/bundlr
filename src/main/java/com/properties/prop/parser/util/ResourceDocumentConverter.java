package com.properties.prop.parser.util;

import com.properties.prop.parser.model.Resource;
import javafx.beans.property.SimpleStringProperty;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class ResourceDocumentConverter {
    public Document convertToDocument(Resource resource){
        Document document=new Document();
        document.add(new TextField("code",resource.getCode(), Field.Store.YES));
        for(Map.Entry<String, SimpleStringProperty> entry : resource.keyValuePairs()){
            document.add(new TextField(entry.getKey(),entry.getValue().get(),Field.Store.YES));
        }
        FieldType fieldType=new FieldType();
        fieldType.setStored(true);
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        Field idField=new Field("id",resource.getId(),fieldType);
        document.add(idField);
        return document;
    }
    public Resource convertToResource(Document document){
        Resource resource=new Resource(document.get("code"),document.get("id"));
        List<IndexableField> fields=document.getFields().stream()
                .filter(Predicate.not(indexableField -> indexableField.name().equals("code") || indexableField.name().equals("id")))
                .collect(Collectors.toList());
        for (IndexableField indexableField : fields) {
            resource.setProperty(indexableField.name(), indexableField.stringValue());
        }
        return resource;
    }
    public List<Document> convertAllToDocument(List<Resource> resources){
        return resources.stream().map(this::convertToDocument).collect(Collectors.toList());
    }
    public List<Resource> convertAllToResource(List<Document> documents){
        return documents.stream().map(this::convertToResource).collect(Collectors.toList());
    }
}
