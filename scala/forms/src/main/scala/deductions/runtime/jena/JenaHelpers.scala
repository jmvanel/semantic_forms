package deductions.runtime.jena

import org.w3.banana.jena.JenaStore
import org.w3.banana.jena.Jena
import org.apache.log4j.Logger
import org.apache.jena.riot.RDFDataMgr
import org.w3.banana.RDFOpsModule
import org.w3.banana.SparqlOps
import org.w3.banana.SparqlHttp
import java.net.URL
import org.w3.banana.jena.JenaModule
import org.w3.banana.Command
import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.rdf.model.ModelFactory
import org.w3.banana.RDFStore

/** Jena Helpers for JenaStore
 *  TODO pave the way for Banana 0.7 :
 *  - JenaStore is not existing anymore
 *  - generic API for transactions */
trait JenaHelpers extends JenaModule {

    /** store URI in a named graph, using Jena's RDFDataMgr
     * (use content-type or else file extension) 
     * with Jena Riot for smart reading of any format,
     * cf https://github.com/w3c/banana-rdf/issues/105
    */
    def storeURI(uri: Rdf#URI, graphUri: Rdf#URI, store: JenaStore ) //  RDFStore[Rdf] )
    : Rdf#Graph
    = {
    store.writeTransaction {
      Logger.getRootLogger().info(s"storeURI uri $uri graphUri $graphUri")
      try{
      	val gForStore = store.getGraph(graphUri)
      	val model = RDFDataMgr.loadModel(uri.toString())
      	
      	// not possible, as Banana also starts a transaction:
//      	store.appendToGraph( uri, model.getGraph() )
      	
      	store.dg.addGraph( uri, model.getGraph() )
      	
      	//	model.getNsPrefixMap // TODO use it to load referred vocab's
      	Logger.getRootLogger().info(s"storeURI uri $uri : stored")
      	model.getGraph
      } catch {
      case t: Throwable => Logger.getRootLogger().error( "ERROR: " + t )
      ModelFactory.createDefaultModel().getGraph
      }
    }
  }
}