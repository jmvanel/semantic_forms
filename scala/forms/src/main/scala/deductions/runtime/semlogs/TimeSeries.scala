package deductions.runtime.semlogs

import java.math.BigInteger
import java.util.Date

import org.w3.banana.RDF

import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.services.SPARQLHelpers

/** swallow and regurgitate user input, to build a history;
 *  a callback is installed in FormSaver via addSaveListener() in ApplicationFacadeImpl */
trait TimeSeries[Rdf <: RDF, DATASET] extends RDFStoreLocalProvider[Rdf, DATASET] 
with LogAPI[Rdf] 
with SPARQLHelpers[Rdf, DATASET] {

  import ops._

  val timestampURI = URI("urn:timestamp")
  val userPredURI = URI("urn:user")
  val metadataGraph = URI( "urn:semantic_forms/metadataGraph" ) // "urn:x-arq:DefaultGraph")


  /** reference implementation of `notifyDataEvent`:
   * save all `addedTriples` to a specific new named graph,
   * and add timestamp metadata to metadata graph `metadataGraph`;
   * by metadata we mean just 2 triples, one telling the user,
   * one for the timestamp, whose subject is the new named graph.
   * These triples are queried in #getMetadata() .
   *
   * This is just an example of what can be saved:
   * another implementation could save other triples (typically an aggregated value), but always
   * in the named graph whose name is computed by makeGraphURIAndMetadata();
   * transactional
   */
  override def notifyDataEvent(addedTriples: Seq[Rdf#Triple],
      removedTriples: Seq[Rdf#Triple], ipAdress: String, isCreation: Boolean)(implicit userURI: String) = {
    // TODO future
    if (!addedTriples.isEmpty)
      rdfStore.rw( dataset2, {
        val (graphUri, metadata ) = makeGraphURIAndMetadata(addedTriples, removedTriples)
        rdfStore.appendToGraph( dataset2, metadataGraph, metadata)
        logger.debug(s"notifyDataEvent: saved to graph <$metadataGraph> : metadata $metadata")

        val graph = makeGraph(addedTriples)
        rdfStore.appendToGraph( dataset2, graphUri, graph)
        logger.debug(s"notifyDataEvent: saved to new graph <$graphUri> : added Triples: $addedTriples")
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

  /**get Metadata for all user updates:
   * subject, timestamp, triple count, user;
   * ordered by recent first;
   * transactional
   * TODO replace userURI (unused) by limit */
  def getMetadata()
    (implicit userURI: String)
        : List[Seq[Rdf#Node]] = {
    val query = s"""
      ${declarePrefix(xsd)}
      SELECT ?SUBJECT (max(?TS) as ?TIME ) (count(?O) AS ?COUNT) ?USER
      WHERE {
        GRAPH <$metadataGraph> {
          ?GR <$timestampURI> ?TS ;
              <$userPredURI> ?USER
              # ; <urn:ip> ?IP ; <urn:action> ?ACTION
              .
        }
        GRAPH ?GR {
         ?SUBJECT ?P ?O . } }
      GROUP BY ?SUBJECT ?USER
      ORDER BY DESC(xsd:integer(?TS))
    """
    logger.debug("getMetadata: query " + query)
    val res = sparqlSelectQueryVariables( query,
        Seq("SUBJECT", "TIME", "COUNT", "USER"), dataset2 )
    logger.debug("getMetadata: res " + res)
    res
  }


  /** get Time Series from accumulated values with timestamp;
   *  used only in https://github.com/jmvanel/corporate_risk and in test
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
    logger.info("getTimeSeries: query " + query)
    val res = sparqlSelectQueryVariables( query, Seq("TS", "AV", "LAB"), dataset2 )
    // res is a  List[Set[Rdf.Node]] each Set containing: Long, Float, String
//    logger.info("res " + res)

    val res2 = res.groupBy{ elem => foldNode(elem.toSeq(2))(
        _=> "", _=> "", lit => fromLiteral(lit)._1 )
    }
    for( (label, values ) <- res2 ) yield {
      val time2value = values . map {
        v =>
//          logger.info( "v " + v )
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
