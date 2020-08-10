package titaniumJena;

import com.apicatalog.rdf.RdfDataset;
import com.apicatalog.rdf.RdfNQuad;
import com.apicatalog.rdf.RdfValue;
import com.apicatalog.rdf.RdfLiteral;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Node;

public class Titanium2Jena {
  public Titanium2Jena() {}

  /** populate a Jena Dataset from a Titanium Dataset */
  public static DatasetGraph populateDataset( RdfDataset titaniumIn, DatasetGraph jenaToUpdate) {
    System.out.println( "jenaToUpdate.supportsTransactions " + jenaToUpdate.supportsTransactions() );
//	if( jenaToUpdate.supportsTransactions() ) jenaToUpdate.begin();
    for ( RdfNQuad triple : titaniumIn.toList() ) {
//      System.out.println( triple.getGraphName().toString() + " " + triple.getSubject() + " <" + triple.getPredicate() + "> " + triple.getObject() );
      RdfValue obj = triple.getObject();
      Node objNode = makeJenaNode(obj);
//	  System.out.println( "Object node " + objNode);
	  Node graphNode;
	  if( triple.getGraphName().isPresent() )
		  graphNode = NodeFactory.createURI( triple.getGraphName().get().getValue() );
	  else
		  graphNode = Quad.defaultGraphIRI;

      jenaToUpdate.add(
        graphNode,
        makeJenaNode(triple.getSubject()),
        NodeFactory.createURI( triple.getPredicate().getValue()),
        objNode );
//      System.out.println( "jenaToUpdate.size " + jenaToUpdate.size() );
    };
//    if( jenaToUpdate.supportsTransactions() ) jenaToUpdate.commit();
    return jenaToUpdate;
  }
  
  private static Node makeJenaNode(RdfValue obj) {
		Node node;
		if (obj.isIRI())
			node = NodeFactory.createURI(obj.getValue());
		else if (obj.isLiteral()) {
			RdfLiteral literal = obj.asLiteral();
			if (literal.getLanguage().isPresent())
				node = NodeFactory.createLiteral(literal.getValue(), literal.getLanguage().get());
			else
				node = NodeFactory.createLiteral(literal.getValue(), NodeFactory.getType(literal.getDatatype()));
		} else
			node = NodeFactory.createBlankNode(obj.getValue());
		return node;
	}
}
