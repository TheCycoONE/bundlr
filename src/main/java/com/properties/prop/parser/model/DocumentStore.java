package com.properties.prop.parser.model;

import com.properties.prop.parser.util.StringUtil;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

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
    public void reloadDocuments(List<Document> documents) throws IOException {
        if(analyzer!=null) {
            clearAll();
            addDocuments(documents);
        }
    }
    public void addDocuments(List<Document> documents)throws IOException {
        if(analyzer!=null) {
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
    public Map<String,List<Document>> searchIndex(String queryString, String[] fieldsArray,String notSortedWord) throws ParseException, IOException {
        List<Document> documents=Collections.emptyList();
        Map<String,List<Document>> fieldDocMap=new LinkedHashMap<>();
        if(!queryString.matches("( +)")&&!queryString.equals("")) {
            if (analyzer != null) {
                for (String field : fieldsArray) {
                    documents = searchIndex(queryString, field, notSortedWord);
                    if (!documents.isEmpty()) {
                        fieldDocMap.put(field, documents);
                    }
                }
            }
        }else {
            if (analyzer != null) {
                for (String field : fieldsArray) {
                    if (!documents.isEmpty()) {
                        fieldDocMap.put(field, Collections.emptyList());
                    }
                }
            }
        }
        return fieldDocMap;
    }

    public List<Document> searchIndex(String queryString,String field,String notSortedWord) throws ParseException, IOException {
        if(analyzer!=null) {
            QueryParser queryParser=new QueryParser(field,analyzer);
            queryParser.setDefaultOperator(QueryParser.Operator.AND);
            if(field.equals(notSortedWord)){
                queryString="*"+queryString+"*";
            }
            List<Document> documents=getDocuments(field,queryString, queryParser);
            if(documents.isEmpty()&&!field.equals(notSortedWord)){
                documents=getDocuments(field,queryString+"*", queryParser);
            }
            String strippedQueryString = queryString.replaceAll("\\*", "");
            if(!documents.isEmpty()) {
                Collections.sort(documents, (o1, o2) -> {
                    double sim1 = StringUtil.positionSimilarity(o1.get(field), strippedQueryString);
                    double sim2 = StringUtil.positionSimilarity(o2.get(field), strippedQueryString);
                    if (sim1 == sim2) {
                        return 0;
                    } else if (sim1 > sim2) {
                        return -1;
                    } else {
                        return 1;
                    }
                });
            }
            return documents;
        }
        return Collections.emptyList();
    }

    public List<Document> searchIndex(String queryString,String field) throws ParseException, IOException {
        if(analyzer!=null) {
            QueryParser queryParser=new QueryParser(field,analyzer);
            queryParser.setDefaultOperator(QueryParser.Operator.OR);
            List<Document> documents=getDocuments(field,queryString, queryParser);
            return documents;
        }
        return Collections.emptyList();
    }

    public List<Document> getAllDocuments() throws IOException {
        Query query=new MatchAllDocsQuery();
        IndexReader reader= DirectoryReader.open(index);
        int numOfDocs=reader.numDocs() !=0 ? reader.numDocs() : 1;
        IndexSearcher searcher = new IndexSearcher(reader);
        return getDocuments(query, numOfDocs,reader,searcher);
    }

    private List<Document> getDocuments(String field,Query query, int hitsPerPage, IndexReader reader) throws IOException {
        IndexSearcher searcher = new IndexSearcher(reader);
        SortField sortField=new SortedNumericSortField("size-"+field, SortField.Type.LONG,false);
        Sort sort=new Sort(sortField);
        TopDocs docs = searcher.search(query, hitsPerPage, sort);
        return getDocuments(reader, searcher, docs);
    }

    private List<Document> getDocuments(String field,String queryString, QueryParser queryParser) throws ParseException, IOException {
        queryParser.setAllowLeadingWildcard(true);
        queryString = StringUtil.escape(queryString);
        Query query=queryParser.parse(queryString);
        int hitsPerPage = 1000;
        IndexReader reader = DirectoryReader.open(index);
        return getDocuments(field,query, hitsPerPage, reader);
    }

    private List<Document> getDocuments(IndexReader reader, IndexSearcher searcher, TopDocs docs) throws IOException {
        ScoreDoc[] hits = docs.scoreDocs;
        List<Document> documents = new ArrayList<>();
        for (ScoreDoc hit : hits) {
            Document document = searcher.doc(hit.doc);
            documents.add(document);
        }
        reader.close();
        return documents;
    }

    private List<Document> getDocuments(Query query, int hitsPerPage, IndexReader reader, IndexSearcher searcher) throws IOException {
        TopDocs docs = searcher.search(query, hitsPerPage);
        return getDocuments(reader, searcher, docs);
    }

}
