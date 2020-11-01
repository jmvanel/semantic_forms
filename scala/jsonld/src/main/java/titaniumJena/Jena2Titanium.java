package titaniumJena;

import com.apicatalog.rdf.RdfDataset;
import com.apicatalog.rdf.RdfGraph;
import com.apicatalog.rdf.RdfNQuad;
import com.apicatalog.rdf.RdfResource;
import com.apicatalog.rdf.RdfTriple;
import com.apicatalog.rdf.RdfValue;
import com.apicatalog.rdf.impl.DefaultRdfProvider;
import com.apicatalog.rdf.spi.RdfProvider;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import com.apicatalog.rdf.RdfDataset;
import org.apache.jena.util.iterator.ExtendedIterator;

public class Jena2Titanium {
	/** populate a Titanium Dataset from a Jena Graph */
	public static RdfDataset populateDataset(Graph jenaIn, RdfDataset titaniumOut) {
		RdfProvider rdfProvider = DefaultRdfProvider.provider();
		ExtendedIterator<Triple> it = jenaIn.find();
		while (it.hasNext()) {
			Triple stmt = it.next();
			RdfResource subject = null;
			if(stmt.getSubject().isURI()) 
				subject = rdfProvider.createIRI(stmt.getSubject().getURI());
			else
				subject = rdfProvider.createBlankNode( stmt.getSubject().getBlankNodeId().getLabelString() );
			RdfResource predicate = rdfProvider.createIRI(stmt.getPredicate().getURI());

			Node jenaObject = stmt.getObject();
			RdfValue object = null;
			if (jenaObject.isURI())
				object = rdfProvider.createIRI(jenaObject.getURI());
			else if (jenaObject.isLiteral()) {
				String lang = jenaObject.getLiteralLanguage();
				String datatype = jenaObject.getLiteralDatatypeURI();
				String lexicalForm = jenaObject.getLiteralLexicalForm();
				if (lang != null && datatype == null)
					object = rdfProvider.createLangString(lexicalForm, lang);
				else
					object = rdfProvider.createTypedString(lexicalForm, datatype);
			} else if (jenaObject.isBlank())
				object = rdfProvider.createBlankNode(jenaObject.getBlankNodeId().getLabelString());

			RdfNQuad nquad = rdfProvider.createNQuad(subject, predicate, object, null);
			titaniumOut.add(nquad);
		}
		return titaniumOut;
	}
}
