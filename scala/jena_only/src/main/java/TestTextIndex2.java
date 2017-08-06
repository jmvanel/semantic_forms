// package deductions.runtime.jena.lucene

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.jena.graph.Factory;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Node_Literal;
import org.apache.jena.graph.Node_URI;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.text.DatasetGraphText;
import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.jena.query.text.TextIndex;
import org.apache.jena.query.text.TextIndexConfig;
import org.apache.jena.query.text.TextIndexException;
import org.apache.jena.query.text.TextIndexLucene;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.tdb.TDBFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;

/**
'd,'fs/$/;/

 * Test Lucene Text Index, no Banana dependency !!!
 *
 *  - initialize TDB with Lucene
 *  - populate TDB with 2 triples
 *  - dump Lucene
 *  - sparql Query
 *  - dump TDB
 */
class TestTextIndex2 {

	  String rdfs = "http://www.w3.org/2000/01/rdf-schema#";
	  String foaf = "http://xmlns.com/foaf/0.1/";

	  Node_URI pred1 = makeUri(rdfs + "label");
	  Node_URI pred2 = makeUri(foaf + "givenName");

	  String directory = "Dataset1";
	  String LUCENEtest = "LUCENEtest2";

	  Dataset dataset;
	  
	  public static void main(String args[]) throws IOException {
		  new TestTextIndex2().run();
	  }
		  
	public void run() throws IOException {

		FileUtils.deleteDirectory(new File(directory));
		FileUtils.deleteDirectory(new File(LUCENEtest));

   	    Dataset dataset0 = TDBFactory.createDataset(directory);
		dataset = configureLuceneIndex(dataset0);

		println("TDB directory = " + directory + " , LUCENE directory = " + LUCENEtest);

		populateTDB();

		TextIndexLucene textIndex = getTextIndex();
		if (textIndex != null) {
			// getTextIndex() match {
			// case Some(textIndex) =>
			println("dump(textIndex=$textIndex)");
			println("textIndex.getDocDef.fields " + textIndex.getDocDef().fields());
			dump(textIndex);
			// case _ =>
		}

		sparqlQuery(query2);
		sparqlQuery(query);
		dataset.close();

		println("tdb.tdbdump (after dataset.close() )");
		tdb.tdbdump.main("--loc", directory);
	}

  ////

  private void println(String string) {
	  System.out.println( string);
}

void populateTDB() {
    try {
      dataset.begin(ReadWrite.WRITE);
      Node_URI s = makeUri("test:/test1");
      Graph graph;
	Node_Literal o;
	Triple tr;
	{
        Node_URI g2 = makeUri("test:/test-extra-data");
        graph = Factory.createDefaultGraph();
        o = makeLiteral("test-extra-data");
        tr = makeTriple(s, pred1, o);
        graph.add(tr);
        dataset.asDatasetGraph().addGraph(g2, graph);
        println(">>>> graph added: $g2 $graph");
      }
      {
        o = makeLiteral("test1");
        tr = makeTriple(s, pred1, o);
        Triple tr2 = makeTriple(s, pred2, o);
        graph = Factory.createDefaultGraph();
        graph.add(tr);
        graph.add(tr2);
        Node_URI g = makeUri("test:/test1");
        dataset.asDatasetGraph().addGraph(g, graph);
        println(">>>> graph added: $g $graph");
      }
      dataset.commit();
    } catch (Exception t) {
        System.err.println("!!!! error " + t.getLocalizedMessage());
    }
    finally {
    	dataset.end();
    }
    println("After transaction");
  }

	Node_URI makeUri(String iriStr) {
		return (Node_URI) NodeFactory.createURI(iriStr);
	}

	Node_Literal makeLiteral(String lexicalForm) {
		return (Node_Literal) NodeFactory.createLiteral(lexicalForm, null, null);
	}

	org.apache.jena.graph.Triple makeTriple(Node s, Node_URI p, Node o) {
		return org.apache.jena.graph.Triple.create(s, p, o);
	}

  /** pasted from jena.textindexdump */
  void dump(TextIndexLucene textIndex) {
    try {
      Directory directory = textIndex.getDirectory();
      Analyzer analyzer = textIndex.getQueryAnalyzer();
      DirectoryReader indexReader = DirectoryReader.open(directory);
      IndexSearcher indexSearcher = new IndexSearcher(indexReader);
      QueryParser queryParser = new QueryParser(
        textIndex.getDocDef().getPrimaryField(), analyzer);
      org.apache.lucene.search.Query query = queryParser.parse("*:*");
      ScoreDoc[] sDocs = indexSearcher.search(query, 1000).scoreDocs;
      printScoreDocs(sDocs, indexSearcher);

      println("search test1");
      org.apache.lucene.search.Query query1 = queryParser.parse("test1");
      ScoreDoc[] res = indexSearcher.search(query1, 1000).scoreDocs;
      printScoreDocs(res, indexSearcher);

    } catch (Exception ex) {
        ex.printStackTrace();
        throw new TextIndexException(ex);
    }
  }

  /** configure Lucene Index for Jena 
 * @throws IOException */
  Dataset configureLuceneIndex(Dataset dataset) throws IOException {
    println(
      "configureLuceneIndex: rdfIndexing getPredicates " +
      rdfIndexing().getPredicates("text") );
    NIOFSDirectory directory = new NIOFSDirectory(Paths.get(LUCENEtest));
    TextIndex textIndex = TextDatasetFactory.createLuceneIndex(
      directory, new TextIndexConfig(rdfIndexing()));
    println("rdfIndex: " + rdfIndexing());
    return TextDatasetFactory.create(dataset, textIndex, true);
  }

  TextIndexLucene getTextIndex() {
    DatasetGraph dsg = dataset.asDatasetGraph();
    println("dsg class : ${dsg.getClass}");
    TextIndexLucene ret;
    if( dsg instanceof DatasetGraphText) {
    	DatasetGraphText dsgText = (DatasetGraphText)dsg;
//    dsg match {
//      case dsgText: DatasetGraphText =>
        println("case dsgText : ${dsg.getClass}");
        TextIndex textIndex = dsgText.getTextIndex();
        println("textIndex class : ${textIndex.getClass}");
//        textIndex match {
//          case textIndex: TextIndexLucene => Some(textIndex)
//          case _                          => None
//        }
        if(textIndex instanceof TextIndexLucene)
        	ret = (TextIndexLucene) textIndex;
        else
            ret = null;
  	
    } else {
//      case _ =>
        println("!!!!!!!!!!! DatasetGraphText not found!");
        ret = null;
    }
    return ret;
  }

  /** cf trait InstanceLabelsInference */
   EntityDefinition rdfIndexing() {
    EntityDefinition entMap = new EntityDefinition("uri", "text", rdfs + "label");
    entMap.setLangField("lang");
    entMap.setUidField("uid");
    entMap.setGraphField("graph");

    entMap.set("text", makeUri(foaf + "givenName"));
    entMap.set("text", makeUri(foaf + "familyName"));
    entMap.set("text", makeUri(foaf + "firstName"));
    entMap.set("text", makeUri(foaf + "lastName"));
    entMap.set("text", makeUri(foaf + "name"));
    entMap.set("text", makeUri(rdfs + "comment"));
    return entMap;
  }

  void printScoreDocs(ScoreDoc[] sDocs, IndexSearcher indexSearcher) throws IOException {
    for (ScoreDoc sd : sDocs) {
      println("Doc: " + sd.doc);
      Document doc = indexSearcher.doc(sd.doc);
      // Don't forget that many fields aren't stored, just indexed.
      int i = 0;
	for ( IndexableField f : doc.getFields() ) {
        i = i + 1;
        println("  $i $f");
        println("  " + f.name() + " = " + f.stringValue());
        //              f.fieldType() )
      }
    }
  }

  String query =
		  "    PREFIX text: <http://jena.apache.org/text#> "
		  + "    SELECT * WHERE {"
		  + "      graph ?g {"
		  + "        ?thing text:query 'test1' ."
		  + "        ?thing ?p ?o ."
		  + "      }"
		  + "    }";

  String query2 =
		  "    PREFIX text: <http://jena.apache.org/text#>"
  		+ "    SELECT * WHERE {"
  		+ "      graph ?g {"
  		+ "        ?thing ?p ?o ."
  		+ "      }"
  		+ "    }";
    
  void sparqlQuery(String query) {
	println("sparql Query " + query);
    dataset.begin(ReadWrite.READ);
    try {
      QueryExecution qExec = QueryExecutionFactory.create(query, dataset);
      ResultSet rs = qExec.execSelect();
      ResultSetFormatter.out(rs);
      dataset.end();
    } catch (Exception ex) {
        ex.printStackTrace();
        dataset.end();
    }
  }
}
