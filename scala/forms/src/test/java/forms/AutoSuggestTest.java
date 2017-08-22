package forms;

// package org.edng.lucene4.example;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.Dictionary;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.search.suggest.analyzing.AnalyzingSuggester;
import org.apache.lucene.search.suggest.analyzing.FreeTextSuggester;
import org.apache.lucene.search.suggest.analyzing.FuzzySuggester;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * cf https://github.com/jmvanel/semantic_forms/issues/75
 * https://github.com/edng/lucene4_cookbook_examples/blob/master/src/main/java/org/edng/lucene4/example/AutoSuggestTest.java
 * https://issues.apache.org/jira/browse/JENA-1250
 * 
 * This class has several tests on auto-suggest feature.
 *
 * Created by ed on 4/1/15.
 */
public class AutoSuggestTest {
    
//	final Version versionLUCENE = Version.LUCENE_4_9;
	
	void fixture(IndexWriter indexWriter) throws Exception {
        Document doc = new Document();
        doc.add(new StringField("content", "Humpty Dumpty sat on a wall", Field.Store.YES));
        indexWriter.addDocument(doc);
        doc = new Document();
        doc.add(new StringField("content", "Humpty Dumpty had a great fall", Field.Store.YES));
        indexWriter.addDocument(doc);
        doc = new Document();
        doc.add(new StringField("content", "All the king's horses and all the king's men", Field.Store.YES));
        indexWriter.addDocument(doc);
        doc = new Document();
        doc.add(new StringField("content", "Couldn't put Humpty together again", Field.Store.YES));
        indexWriter.addDocument(doc);

        indexWriter.commit();
        indexWriter.close();
	}

    @org.junit.Test
    public void analyzingSuggesterTest() throws Exception {
    	System.out.println( "\nanalyzingSuggesterTest" );

        StandardAnalyzer analyzer = new StandardAnalyzer();
        Directory directory = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, config);

        fixture(indexWriter);

        IndexReader indexReader = DirectoryReader.open(directory);

        Dictionary dictionary = new LuceneDictionary(indexReader, "content");

        AnalyzingSuggester analyzingSuggester = new AnalyzingSuggester(directory, "analyzingSuggester", new StandardAnalyzer());
        analyzingSuggester.build(dictionary);

        List<Lookup.LookupResult> lookupResultList = analyzingSuggester.lookup("humpty dum", false, 10);

        assertEquals("Number of hits matching", 2, lookupResultList.size(), 0);

        for (Lookup.LookupResult lookupResult : lookupResultList) {
            System.out.println(lookupResult.key + ": " + lookupResult.value);
        }

    }

    @org.junit.Test
    public void analyzingInfixSuggesterTest() throws Exception {
    	System.out.println( "\nanalyzingInfixSuggesterTest" );

        StandardAnalyzer analyzer = new StandardAnalyzer();
        Directory directory = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, config);

        fixture(indexWriter);

        IndexReader indexReader = DirectoryReader.open(directory);

        Dictionary dictionary = new LuceneDictionary(indexReader, "content");

        AnalyzingInfixSuggester analyzingInfixSuggester = new AnalyzingInfixSuggester( directory, analyzer);
        analyzingInfixSuggester.build(dictionary);

        List<Lookup.LookupResult> lookupResultList = analyzingInfixSuggester.lookup("put h", false, 10);
        analyzingInfixSuggester.close();

        assertEquals("Number of hits matching", 1, lookupResultList.size(), 0);

        for (Lookup.LookupResult lookupResult : lookupResultList) {
            System.out.println(lookupResult.key + ": " + lookupResult.value);
        }

    }

    @org.junit.Test
    public void freeTextSuggesterTest() throws Exception {
    	System.out.println( "\nfreeTextSuggester" );

        StandardAnalyzer analyzer = new StandardAnalyzer();
        Directory directory = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, config);

        fixture(indexWriter);

        IndexReader indexReader = DirectoryReader.open(directory);

        Dictionary dictionary = new LuceneDictionary(indexReader, "content");

        FreeTextSuggester freeTextSuggester = new FreeTextSuggester(analyzer, analyzer, 3);
        freeTextSuggester.build(dictionary);

        List<Lookup.LookupResult> lookupResultList = freeTextSuggester.lookup("h", false, 10);

        assertEquals("Number of hits matching", 3, lookupResultList.size(), 0);

        for (Lookup.LookupResult lookupResult : lookupResultList) {
            System.out.println(lookupResult.key + ": " + lookupResult.value);
        }

    }

    @org.junit.Test
    public void fuzzySuggesterTest() throws Exception {
    	System.out.println( "\nfuzzySuggesterTest" );

        StandardAnalyzer analyzer = new StandardAnalyzer();
        Directory directory = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, config);

        fixture(indexWriter);

        IndexReader indexReader = DirectoryReader.open(directory);

        Dictionary dictionary = new LuceneDictionary(indexReader, "content");

        FuzzySuggester fuzzySuggester = new FuzzySuggester(directory, "fuzzySuggester", new StandardAnalyzer());
        fuzzySuggester.build(dictionary);

        List<Lookup.LookupResult> lookupResultList = fuzzySuggester.lookup("hampty", false, 10);

        assertEquals("Number of hits not matching", 2, lookupResultList.size(), 0);

        for (Lookup.LookupResult lookupResult : lookupResultList) {
            System.out.println(lookupResult.key + ": " + lookupResult.value);
        }

    }
}

