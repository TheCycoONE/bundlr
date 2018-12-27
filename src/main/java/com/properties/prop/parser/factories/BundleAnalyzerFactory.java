package com.properties.prop.parser.factories;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class BundleAnalyzerFactory implements AnalyzerFactory{

    @Override
    public Analyzer createAnalyzer() {
        Map<String,Analyzer> fieldAnalyzerMap=new HashMap<>();
        fieldAnalyzerMap.put("id",new KeywordAnalyzer());
        Analyzer perFieldAnalyzer=new PerFieldAnalyzerWrapper(new StandardAnalyzer(),fieldAnalyzerMap);
        return perFieldAnalyzer;
    }
}
