package com.properties.prop.parser.factories;

import org.apache.lucene.analysis.Analyzer;

public interface AnalyzerFactory {
    Analyzer createAnalyzer();
}
