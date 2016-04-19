package deductions.runtime.sparql_cache.algos

import org.w3.banana.RDF
import org.w3.banana.io.RDFLoader
import scala.util.Try
import java.net.URL
import org.w3.banana.RDFOps
import org.w3.banana.FOAFPrefix
import org.w3.banana.Prefix
import org.w3.banana.RDFSPrefix
import org.w3.banana.io.RDFWriter
import org.w3.banana.io.NTriples
import java.nio.file.StandardOpenOption

trait ChildrenFetcher[Rdf <: RDF]
    extends RDFLoader[Rdf, Try] {

  implicit val ops: RDFOps[Rdf]
  import ops._

  /** fetch documents ?D such that:
   *  <url> ?P ?D .
   * where ?P is in list propertyFilter */
  def fetch(url: URL, propertyFilter: List[Rdf#URI]): List[Rdf#Graph] = {
    val graph = load(url).getOrElse(emptyGraph)
    val v = getTriples(graph) collect {
      case triple if (propertyFilter.contains(triple.predicate) || propertyFilter.isEmpty) =>
        val doc = load(new URL(triple.objectt.toString())).getOrElse(emptyGraph)
        println( s"Fetched object from triple $triple, size ${doc.size}" )
        doc
    }
    v.toList
  }

  def fetch(url: URL, propertyFilter: List[Rdf#URI],
            propertyFilter2: List[Rdf#URI]): List[Rdf#Triple] = {
    val grs = fetch(url, propertyFilter)

    val trss = for {
      gr <- grs
      pr <- propertyFilter2
      trs = find(gr, ANY, pr, ANY)
    } yield trs
    trss.flatten
  }

  
  lazy val foafPrefix = "http://xmlns.com/foaf/0.1/"
  lazy val foaf = FOAFPrefix[Rdf]
  lazy val cco = Prefix[Rdf]("cco", "http://purl.org/ontology/cco/core#")
  lazy val rdfs = RDFSPrefix[Rdf]
  lazy val dbo = Prefix[Rdf]("dbo", "http://dbpedia.org/ontology/")

  implicit val ntriplesWriter: RDFWriter[Rdf, Try, NTriples]

  /** will be used for machine learning */
  def fetchDBPediaAbstractFromInterestsAndExpertise(url: URL): List[Rdf#Triple] = {
    fetch(url, List(foaf.topic_interest, cco("cco:expertise")), List(rdfs.label, dbo("abstract")))
  }
  
  def writeToNTriplesFile( triples: List[Rdf#Triple], file: String = "dump1.nt", base: String = "urn:sample" ) = {
	  import java.nio.file.{Paths, Files}
	  import java.nio.charset.StandardCharsets
	  val writer = Files.newBufferedWriter( Paths.get(file), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW )
    for( tr <- triples ) {
      writer.write( ntriplesWriter.asString( Graph(triples), base ).get )
    }
	  writer.close()
  }
}