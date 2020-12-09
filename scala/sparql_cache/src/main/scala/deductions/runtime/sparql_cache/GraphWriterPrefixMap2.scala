package deductions.runtime.sparql_cache

import java.io.OutputStream
import java.util.Map
import java.io.ByteArrayOutputStream
import java.util.HashMap
import org.apache.jena.graph.Graph
import org.apache.jena.riot.RDFFormat
import org.apache.jena.riot.RDFWriterRegistry
import org.apache.jena.sparql.util.Context
import org.apache.jena.util.PrefixMappingUtils
import org.apache.jena.shared.impl.PrefixMappingImpl
import org.apache.jena.riot.system.PrefixMapBase
import org.w3.banana.RDF
import org.w3.banana.jena.Jena
import org.w3.banana.RDFOps

trait GraphWriterPrefixMapTrait[Rdf <: RDF]{
  def writeGraph(graph: Rdf#Graph, out: OutputStream = new ByteArrayOutputStream,
                 rdfFormat: RDFFormat           = RDFFormat.TURTLE_PRETTY,
                 prefixMap: Map[String, String] = new HashMap): OutputStream
}

/**
 * Graph Writer with Prefix Map, using Jena Riot
 * wrapper of the Jena implementation ot be used in a Banana RDF context
 */
class GraphWriterPrefixMapClass (implicit ops: RDFOps[Jena])
extends GraphWriterPrefixMapTrait[Jena]
with GraphWriterPrefixMap {}
