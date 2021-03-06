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

/**
 * Graph Writer with Prefix Map, using Jena Riot
 *  TODO add base URI when needed
 */
trait GraphWriterPrefixMap {
  def writeGraph(graph: Graph, out: OutputStream = new ByteArrayOutputStream,
                 rdfFormat: RDFFormat           = RDFFormat.TURTLE_PRETTY,
                 prefixMap: Map[String, String] = new HashMap,
                 baseURI:String = ""): OutputStream = {
    val writerFactory = RDFWriterRegistry.getWriterGraphFactory(rdfFormat)
    val pmi = new PrefixMappingImpl
    pmi.setNsPrefixes(prefixMap)
    val prefixMapping = PrefixMappingUtils.calcInUsePrefixMapping(graph, pmi)
    val graphWriter = writerFactory.create(rdfFormat)
    val prefixMapImpl = new PrefixMapImpl(prefixMapping.getNsPrefixMap)
    graphWriter.write(out, graph,
      prefixMapImpl,
      // PrefixMapFactory.createForOutput(prefixMap), // this would output the whole prefixMap
      baseURI, new Context())
    return out
  }

  class PrefixMapImpl( prefixMap: Map[String, String] ) extends PrefixMapBase {
          def getMapping(): java.util.Map[String, String] = prefixMap
      override def abbrev(uriStr: String): org.apache.jena.atlas.lib.Pair[String, String] =
        this.abbrev( getMapping(), uriStr, true)
      /** @return URI in prefixed name form if possible, null otherwise */
      override def abbreviate(uriStr: String): String = {
        val abbrevPair = abbrev(uriStr)
        if( abbrevPair != null ){
        val prefix = abbrevPair.getLeft
        val localPart = abbrevPair.getRight
        if( prefix != null && localPart != null )
          s"$prefix:$localPart"
        else null
        } else null
      }
      def containsPrefix(prefix: String): Boolean = getMapping().containsKey(prefix)
      def isEmpty(): Boolean = this.getMapping().isEmpty()
      def size(): Int = this.getMapping().size()
      
      def add(x$1: String, x$2: String): Unit = ???
      def clear(): Unit = ???
      def delete(x$1: String): Unit = ???
      override def expand(x$1: String, x$2: String): String = ???

      def get(s: String): String = ???
  }
}
