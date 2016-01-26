package deductions.runtime.semlogs

import org.w3.banana.RDF
import deductions.runtime.dataset.RDFStoreLocalProvider
import java.util.Date
import org.w3.banana.RDFSPrefix
import deductions.runtime.services.SPARQLHelpers
import java.math.BigInteger

/** swallow and regurgitate user input, to build a history;
 *  a callback is installed in FormSaver via addSaveListener() in ApplicationFacadeImpl */
trait TimeSeries[Rdf <: RDF, DATASET] extends RDFStoreLocalProvider[Rdf, DATASET] 
with LogAPI[Rdf] 
with SPARQLHelpers[Rdf, DATASET] {

  import ops._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._
  import rdfStore.sparqlEngineSyntax._
  import scala.concurrent.ExecutionContext.Implicits.global

  val timestampURI = URI("urn:timestamp")
  val userPredURI = URI("urn:user")
  val metadataGraph = URI( "urn:semantic_forms/metadataGraph" ) // "urn:x-arq:DefaultGraph")

  private val rdfs = RDFSPrefix[Rdf]
  
  /** reference implementation:
   * save all `addedTriples` to a specific new named graph,
   * and add timestamp metadata to metadata graph;
   * this is just an example:
   * another implementation could save other triples (typically an aggregated value), but always
   * in the named graph whose name is computed by makeGraphURIAndMetadata();
   * transactional
   */
  override def notifyDataEvent(addedTriples: Seq[Rdf#Triple],
      removedTriples: Seq[Rdf#Triple])(implicit userURI: String) = {
    // TODO future
    if (!addedTriples.isEmpty)
      dataset2.rw({
        val (graphUri, metadata ) = makeGraphURIAndMetadata(addedTriples, removedTriples)
        dataset2.appendToGraph( metadataGraph, metadata)
       
        // TODO: add only modified triples (configurable)
        val graph = makeGraph(addedTriples)
        dataset2.appendToGraph(graphUri, graph)
      })
      Unit
  }

  /** make Graph URI And associated metadata for saving data at a current date & time;
   * the graph URI is for saving actual data in this named graph;
   * the metadata has the graph URI as subject */
  def makeGraphURIAndMetadata(addedTriples: Seq[Rdf#Triple], 
      removedTriples: Seq[Rdf#Triple])(implicit userURI: String): (Rdf#URI, Rdf#Graph)= {
	  val timestamp = (new Date).getTime
			  val graphName = addedTriples.head.subject.toString() + "#" + timestamp
			  val graphUri = URI(graphName)
        val metadata = (graphUri
          -- timestampURI ->- Literal(timestamp.toString(), xsd.integer )
        -- userPredURI ->- URI(userURI)).graph
         ( graphUri, metadata ) 
  }

  /**get Metadata for user updates:
   * subject, timestamp, triple count;
   * ordered by recent first;
   * transactional */
  def getMetadata()
    (implicit userURI: String)
        : List[Seq[Rdf#Node]] = {
    val query = s"""  # DISTINCT
      SELECT ?SUBJECT (max(?TS) as ?TIME ) (count(?O) AS ?COUNT)
      WHERE {
        GRAPH <urn:semantic_forms/metadataGraph> {
          ?GR <urn:timestamp> ?TS . }
        GRAPH ?GR {
         ?SUBJECT ?P ?O . } }
      GROUP BY ?SUBJECT
      ORDER BY DESC(?TS)
    """
    println("getMetadata: query " + query)
    val res = sparqlSelectQueryVariables( query, Seq("SUBJECT", "TIME", "COUNT"), dataset2 )
    println("getMetadata: res " + res)
    res
  }


  /** get Time Series from accumulated values with timestamp;
   *  used only in https://github.com/jmvanel/corporate_risk
   *  @return a Map from label to a seq of time & value pairs;
   *  the time is a Unix time obtained by Date.getTime,
   *  the value is a double;
   * NON transactional
   */
  def getTimeSeries( predicateURI: String = "urn:average")(implicit userURI: String):
//  Seq[( String, Map[Long, Float] )] = {
  Map[ String, Seq[(BigInteger, Double)] ] = {
    val query = s"""
      SELECT ?TS ?AV ?LAB
      WHERE {
        GRAPH <$metadataGraph> {
          ?GR <$timestampURI> ?TS ;
            <$userPredURI> <$userURI> .
        }
        GRAPH ?GR {
         ?S <$predicateURI> ?AV ;
            <${rdfs.label}> ?LAB .
        }
      }  """
    println("query " + query)
    val res = sparqlSelectQueryVariables( query, Seq("TS", "AV", "LAB"), dataset2 )
    // res is a  List[Set[Rdf.Node]] each Set containing: Long, Float, String
//    println("res " + res)

    val res2 = res.groupBy{ elem => foldNode(elem.toSeq(2))(
        _=> "", _=> "", lit => fromLiteral(lit)._1 )
    }
    for( (label, values ) <- res2 ) yield {
      val time2value = values . map {
        v =>
//          println( "v " + v )
          val vv = v.toSeq ; (
            vv(0).as[BigInteger].get,
            vv(1).as[Double].get )
      }
      ( label, time2value )
    }
  }
  
  // TODO move to helper class  
  def makeStringFromLiteral(n: Rdf#Node): String = {
    foldNode(n)(
        _ => "",
        _ => "",
        literal => fromLiteral(literal)._1 )
  }
}
