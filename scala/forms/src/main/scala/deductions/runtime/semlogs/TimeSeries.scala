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
  
  trait SaveDataEventListener extends SaveListener {
    
    /** save `addedTriples` to a specific new named graph,
     *  and add timestamp metadata to default graph */
    override def notifyDataEvent(addedTriples: Seq[Rdf#Triple], removedTriples: Seq[Rdf#Triple])
    (implicit userURI: String)
    = {
      // TODO future
      dataset2.rw({
        val graphName = "" // TODO
        val graphUri = URI(graphName)
        val graph = makeGraph(addedTriples)
        dataset2.appendToGraph(graphUri, graph)
        
        val timestamp = (new Date).getTime
        val metadata = ( graphUri
        -- URI("timestamp") ->- Literal( timestamp.toString() )
        -- URI("user") ->- URI(userURI)
        ) . graph
        dataset2.appendToGraph( URI(""), metadata)
      })
    }
  }

  //    /** function arguments are like in notifyEditEvent():
  //     * userURI: String, subjectURI: String, propURI: String, objectURI: String  */
  //    def setSavedDataFunctionEditEvent(f:
  //        (String, String, String, String) =>
  //          PointedGraph[Rdf] =
  //            // by default store reified triple
  //          (u: String, s: String, p: String, o: String) => (
  //        		  URI(s) -- URI(p) ->- Literal(o) ))
}