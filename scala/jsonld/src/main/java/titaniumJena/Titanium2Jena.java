package titaniumJena;

import com.apicatalog.rdf.RdfDataset;
import com.apicatalog.rdf.RdfNQuad;
import com.apicatalog.rdf.RdfValue;
import com.apicatalog.rdf.RdfLiteral;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Node;

public class Titanium2Jena {
  public Titanium2Jena() {}

  /** populate a Jena Dataset from a Titanium Dataset */
  public DatasetGraph populateDataset( RdfDataset titaniumIn, DatasetGraph jenaToUpdate) {   
    for ( RdfNQuad triple : titaniumIn.toList() ) {
      // System.out.println ( triple.getSubject() + " <" + triple.getPredicate() + "> " + triple.getObject() );
      RdfValue obj = triple.getObject();
      Node objNode;
      if( obj . isIRI() )
        objNode = NodeFactory.createURI(obj.getValue());
      else if( obj . isLiteral() ) {
        RdfLiteral literal = obj.asLiteral();
	if(literal.getLanguage().isPresent()) 
          objNode = NodeFactory.createLiteral(
            literal.getValue(),
            literal.getLanguage().get() );
	else
          objNode = NodeFactory.createLiteral(
            literal.getValue(),
            NodeFactory.getType(literal.getDatatype()) );
      } else
        objNode = NodeFactory.createBlankNode(obj.getValue());

      jenaToUpdate.add(
        // TODO NodeFactory.createURI( triple.getGraphName().getValue()),
        null,
        NodeFactory.createURI( triple.getSubject().getValue()),
        NodeFactory.createURI( triple.getPredicate().getValue()),
        objNode );
    };
    return jenaToUpdate;
  }
}
