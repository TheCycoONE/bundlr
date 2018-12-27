package com.properties.prop.parser.factories;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.ar.ArabicAnalyzer;
import org.apache.lucene.analysis.bg.BulgarianAnalyzer;
import org.apache.lucene.analysis.bn.BengaliAnalyzer;
import org.apache.lucene.analysis.ca.CatalanAnalyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.da.DanishAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.eu.BasqueAnalyzer;
import org.apache.lucene.analysis.fa.PersianAnalyzer;
import org.apache.lucene.analysis.fi.FinnishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.ga.IrishAnalyzer;
import org.apache.lucene.analysis.gl.GalicianAnalyzer;
import org.apache.lucene.analysis.hi.HindiAnalyzer;
import org.apache.lucene.analysis.hu.HungarianAnalyzer;
import org.apache.lucene.analysis.hy.ArmenianAnalyzer;
import org.apache.lucene.analysis.id.IndonesianAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.lt.LithuanianAnalyzer;
import org.apache.lucene.analysis.lv.LatvianAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.no.NorwegianAnalyzer;
import org.apache.lucene.analysis.pt.PortugueseAnalyzer;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.sv.SwedishAnalyzer;
import org.apache.lucene.analysis.th.ThaiAnalyzer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ResourceAnalyzerFactory implements LocalizedAnalyzerFactory {

    @Override
    public Analyzer createLocalizedAnalyzer(Set<String> languageFiles) {
        Map<String,Analyzer> fieldAnalyzerMap=languageFiles.stream().collect(Collectors.toMap(Function.identity(),this::createLanguageAnalyzer));
        fieldAnalyzerMap.put("id",new KeywordAnalyzer());
        Analyzer perFieldAnalyzer=new PerFieldAnalyzerWrapper(new StandardAnalyzer(),fieldAnalyzerMap);
        return perFieldAnalyzer;
    }
    private Analyzer createLanguageAnalyzer(String languageFile){
        int nameLength=languageFile.length();
        String language=languageFile.substring(nameLength-5,nameLength-3);
        switch (language){
            case "fr":
                return new FrenchAnalyzer(CharArraySet.EMPTY_SET);
            case "en":
                return new EnglishAnalyzer(CharArraySet.EMPTY_SET);
            case "ro":
                return new RomanianAnalyzer(CharArraySet.EMPTY_SET);
            case "es":
                return new SpanishAnalyzer(CharArraySet.EMPTY_SET);
            case "de":
                return new GermanAnalyzer(CharArraySet.EMPTY_SET);
            case "hu":
                return new HungarianAnalyzer(CharArraySet.EMPTY_SET);
            case "hi":
                return new HindiAnalyzer(CharArraySet.EMPTY_SET);
            case "it":
                return new ItalianAnalyzer(CharArraySet.EMPTY_SET);
            case "id":
                return new IndonesianAnalyzer(CharArraySet.EMPTY_SET);
            case "fi":
                return new FinnishAnalyzer(CharArraySet.EMPTY_SET);
            case "lv":
                return new LatvianAnalyzer(CharArraySet.EMPTY_SET);
            case "nl":
                return new DutchAnalyzer(CharArraySet.EMPTY_SET);
            case "no":
                return new NorwegianAnalyzer(CharArraySet.EMPTY_SET);
            case "cz":
                return new CzechAnalyzer(CharArraySet.EMPTY_SET);
            case "da":
                return new DanishAnalyzer(CharArraySet.EMPTY_SET);
            case "el":
                return new GreekAnalyzer(CharArraySet.EMPTY_SET);
            case "eu":
                return new BasqueAnalyzer(CharArraySet.EMPTY_SET);
            case "fa":
                return new PersianAnalyzer(CharArraySet.EMPTY_SET);
            case "ga":
                return new IrishAnalyzer(CharArraySet.EMPTY_SET);
            case "gl":
                return new GalicianAnalyzer(CharArraySet.EMPTY_SET);
            case "hy":
                return new ArmenianAnalyzer(CharArraySet.EMPTY_SET);
            case "lt":
                return new LithuanianAnalyzer(CharArraySet.EMPTY_SET);
            case "pt":
                return new PortugueseAnalyzer(CharArraySet.EMPTY_SET);
            case "ru":
                return new RussianAnalyzer(CharArraySet.EMPTY_SET);
            case "sv":
                return new SwedishAnalyzer(CharArraySet.EMPTY_SET);
            case "th":
                return new ThaiAnalyzer(CharArraySet.EMPTY_SET);
            case "bg":
                return new BulgarianAnalyzer(CharArraySet.EMPTY_SET);
            case "ar":
                return new ArabicAnalyzer(CharArraySet.EMPTY_SET);
            case "bn":
                return new BengaliAnalyzer(CharArraySet.EMPTY_SET);
            case "ca":
                return new CatalanAnalyzer(CharArraySet.EMPTY_SET);
            case "ch":
                return new CJKAnalyzer(CharArraySet.EMPTY_SET);
            case "jp":
                return new CJKAnalyzer(CharArraySet.EMPTY_SET);
            case "kr":
                return new CJKAnalyzer(CharArraySet.EMPTY_SET);
            default:
                return new StandardAnalyzer(CharArraySet.EMPTY_SET);
        }
    }
}
