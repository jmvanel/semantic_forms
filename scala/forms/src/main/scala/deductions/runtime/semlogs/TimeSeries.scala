package deductions.runtime.semlogs

import org.w3.banana.RDF
import deductions.runtime.dataset.RDFStoreLocalProvider
import java.util.Date
import org.w3.banana.RDFSPrefix
import deductions.runtime.services.SPARQLHelpers
import java.math.BigInteger

trait TimeSeries[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with LogAPI[Rdf]
    with SPARQLHelpers[Rdf, DATASET] {

  import ops._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._
  import rdfStore.sparqlEngineSyntax._
  import scala.concurrent.ExecutionContext.Implicits.global

  /**
   * save `addedTriples` to a specific new named graph,
   *  and add timestamp metadata to default graph
   */
  override def notifyDataEvent(addedTriples: Seq[Rdf#Triple],
      removedTriples: Seq[Rdf#Triple])(implicit userURI: String) = {
    // TODO future
    if (!addedTriples.isEmpty)
      dataset2.rw({
        val (graphUri, metadata ) = makeGraphURIAndMetadata(addedTriples, removedTriples)
        dataset2.appendToGraph(URI(""), metadata)
       
        val graph = makeGraph(addedTriples)
        dataset2.appendToGraph(graphUri, graph)        
      })
      Unit
  }

  def makeGraphURIAndMetadata(addedTriples: Seq[Rdf#Triple],
      removedTriples: Seq[Rdf#Triple])(implicit userURI: String) = {
        	  val timestamp = (new Date).getTime
        val graphName = addedTriples.head.subject.toString() + "#" + timestamp
        val graphUri = URI(graphName)
        val metadata = (graphUri
          -- URI("timestamp") ->- Literal(timestamp.toString())
          -- URI("user") ->- URI(userURI)).graph
         ( graphUri, metadata ) 
  }
  
  private val rdfs = RDFSPrefix[Rdf]
  
  /** get Time Series from accumulated values with timestamp */
  def getTimeSeries()(implicit userURI: String):
//  Seq[( String, Map[Long, Float] )] = {
  Map[ String, Seq[(BigInteger, Double)] ] = {
    val query = s"""
      SELECT ?TS ?AV ?LAB
      WHERE {
        ?GR <timestamp> ?TS ;
            <user> <$userURI> .
        GRAPH ?GR {
         ?S <average> ?AV ;
            <${rdfs.label}> ?LAB .
        }
      }  """    
    val res = sparqlSelectQuery( query, dataset2 ) . get
    // res is a  List[Set[Rdf.Node]] each Set containing:
    // Long, Float, String
    val res2 = res.groupBy{ elem => foldNode(elem.toSeq(2))(
        _=> "", _=> "", lit => fromLiteral(lit)._1 )
    }
    for( (label, values ) <- res2 ) yield {
      val time2value = values . map {
//        v => val vv = v.toSeq ; ( vv(0).as[Long], vv(1).as[Float] )
        v => val vv = v.toSeq ; ( vv(0).as[BigInteger].get,
            vv(1).as[Double].get )
      }
      ( label, time2value )
    }
  }
  
  // TODO move to helper class  
  private def makeStringFromLiteral(n: Rdf#Node): String = {
    foldNode(n)(
        _ => "",
        _ => "",
        literal => fromLiteral(literal)._1 )
  }
}