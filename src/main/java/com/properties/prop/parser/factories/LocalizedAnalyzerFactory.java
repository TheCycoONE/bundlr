package com.properties.prop.parser.factories;

import org.apache.lucene.analysis.Analyzer;

import java.util.Set;

public interface LocalizedAnalyzerFactory{
    Analyzer createLocalizedAnalyzer(Set<String> languages);
}
