package deductions.runtime.semlogs

import org.w3.banana.RDF
import deductions.runtime.dataset.RDFStoreLocalProvider
import java.util.Date

trait TimeSeries[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with LogAPI[Rdf] {

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
}