package com.properties.prop.parser.model;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DocumentStore {
    private Directory index;
    private String indexLocation;

    private Analyzer analyzer;
    public DocumentStore(String storeName) throws IOException {
        indexLocation=System.getProperty("user.dir")+"/indexes/"+storeName;
        index = FSDirectory.open(Path.of(indexLocation));
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(Analyzer analyzer){
        this.analyzer=analyzer;
    }
    public void clearAll() throws IOException {
        if(analyzer!=null) {
            if (DirectoryReader.indexExists(index)) {
                IndexWriterConfig config = new IndexWriterConfig(analyzer);
                IndexWriter writer = new IndexWriter(index, config);
                writer.deleteAll();
                writer.commit();
                writer.close();
            }
            String[] leftOverFiles = index.listAll();
            if (leftOverFiles.length != 0) {
                for (String file : leftOverFiles) {
                    index.deleteFile(file);
                }
            }
            FileUtils.deleteDirectory(new File(indexLocation));
        }
    }
    public void addDocuments(List<Document> documents) throws IOException {
        if(analyzer!=null) {
            clearAll();
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            IndexWriter writer = new IndexWriter(index, config);
            for (Document document : documents) {
                writer.addDocument(document);
            }
            writer.commit();
            writer.close();
        }
    }
    public void addDocument(Document document) throws IOException {
        if(analyzer!=null) {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            IndexWriter writer = new IndexWriter(index, config);
            writer.addDocument(document);
            writer.commit();
            writer.close();
        }
    }
    public void updateDocument(String key,String value,Document document) throws IOException {
        if(analyzer!=null) {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            IndexWriter writer = new IndexWriter(index, config);
            List<IndexableField> fields = document.getFields();
            writer.updateDocument(new Term(key, value), document);
            writer.forceMergeDeletes();
            writer.commit();
            writer.close();
        }
    }
    public void deleteDocument(String key,String value) throws IOException {
        if(analyzer!=null) {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            IndexWriter writer = new IndexWriter(index, config);
            writer.deleteDocuments(new Term(key, value));
            writer.forceMergeDeletes();
            writer.commit();
            writer.close();
        }
    }
    public void deleteDocuments(String key,List<String> values) throws IOException {
        for(String value : values){
            deleteDocument(key,value);
        }
    }
    public List<Document> searchIndex(String queryString, String[] fieldsArray) throws ParseException, IOException {
        if(analyzer!=null) {
            MultiFieldQueryParser queryParser = new MultiFieldQueryParser(fieldsArray, analyzer);
            queryParser.setDefaultOperator(QueryParser.Operator.OR);
            queryParser.setAllowLeadingWildcard(true);
            Query query = queryParser.parse(queryString);
            int hitsPerPage = 1000;
            IndexReader reader = DirectoryReader.open(index);
            return getDocuments(query, hitsPerPage, reader);
        }
        return Collections.emptyList();
    }

    public List<Document> getAllDocuments() throws IOException {
        Query query=new MatchAllDocsQuery();
        IndexReader reader= DirectoryReader.open(index);
        int numOfDocs=reader.numDocs() !=0 ? reader.numDocs() : 1;
        return getDocuments(query, numOfDocs, reader);
    }

    private List<Document> getDocuments(Query query, int hitsPerPage, IndexReader reader) throws IOException {
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs docs = searcher.search(query, hitsPerPage);
        ScoreDoc[] hits = docs.scoreDocs;
        List<Document> documents = new ArrayList<>();
        for (ScoreDoc hit : hits) {
            Document document = searcher.doc(hit.doc);
            documents.add(document);
        }
        reader.close();
        return documents;
    }

}
