import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.sparql.core.DatasetGraph;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.Factory;
import org.apache.jena.util.iterator.ExtendedIterator;

public class TestGraphIRI {

	static Node s = NodeFactory.createURI("http://uri");
  static Node p = NodeFactory.createURI("http://prop");
  static Node o = NodeFactory.createURI("http://Théodore_Monod");
  static Node o2= NodeFactory.createURI("http://Victor_Hugo");
  static String directory = "DatasetTest" ;

	public static void main(String[] args) {
    Dataset dataset = TDBFactory.createDataset(directory) ;
    dataset.begin(ReadWrite.WRITE) ;
    DatasetGraph dg = dataset.asDatasetGraph();
 
    // like Banana makeGraph
    Graph graph = Factory.createDefaultGraph();
    Triple tr = Triple.create(s, p, o);
    graph.add(tr);
    Triple tr2 = Triple.create(s, p, o2);
    graph.add(tr2);

    // like Banana appendToGraph
    ExtendedIterator<Triple> it = graph.find(Node.ANY, Node.ANY, Node.ANY);
    System.out.println("After graph.find(ANY,ANY,ANY) // like Banana getTriples() ");
    System.out.println( it.next() );
    it = graph.find(Node.ANY, Node.ANY, Node.ANY);
    while (it.hasNext()) {
      Triple tr3 = it.next();
      dg.add(tr3.getSubject(), tr3.getSubject(), tr3.getPredicate(), tr3.getObject() );
    }
    dataset.commit() ;
////
    dataset.begin(ReadWrite.READ) ;
    System.out.println("dataset.asDatasetGraph() , after dg.add() in TDB");
    System.out.println(dataset.asDatasetGraph().toString());
    System.out.println( System.getProperty("user.dir"));
    dataset.end() ;
	}

	/** UNUSED, does not show problem */
  private static void testDatasetGraph(DatasetGraph dg) {
    dg.add(s, s,p,o);
    Graph gr = dg.getGraph(s);
    System.out.println( gr.toString() );
  }

	/** UNUSED, does not show problem */
  private static void testModel(Dataset dataset) {
    Model model = dataset.getDefaultModel() ;
    Resource s = model.createResource("http://uri");
    Property p = model.createProperty("http://prop");
    Resource o = model.createResource("http://Théodore_Monod");
    Statement stat = model.createStatement(s, p, o);
    model.add(stat);
    StmtIterator it = model.listStatements();
    System.out.println( it.next() );
  }
}
