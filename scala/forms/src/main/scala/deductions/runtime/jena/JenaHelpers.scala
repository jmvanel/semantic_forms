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
import org.w3.banana.diesel._

/** Jena Helpers for JenaStore
 *  TODO pave the way for Banana 0.7 :
 *  - JenaStore is not existing anymore
 *  - generic API for transactions */
trait JenaHelpers extends JenaModule2 {
	import Ops._

    /** store URI in a named graph, using Jena's RDFDataMgr
     * (use content-type or else file extension) 
     * with Jena Riot for smart reading of any format,
     * cf https://github.com/w3c/banana-rdf/issues/105
    */
    def storeURI_new(uri: Rdf#URI, graphUri: Rdf#URI, dataset : RDFStoreObject.DATASET ) //  RDFStore[Rdf] )
    : Rdf#Graph = {
    rdfStore.rw( dataset, {      
//    store.writeTransaction {
      Logger.getRootLogger().info(s"storeURI uri $uri graphUri $graphUri")
      try{
        val dataset =  RDFStoreObject.dataset
      	val gForStore = rdfStore.getGraph(graphUri)
        // read from uri no matter what the syntax is:
      	val model = RDFDataMgr.loadModel(uri.toString())
      	
      	// TODO not possible, as Banana 0.6 also starts a transaction ???      	
      	rdfStore. appendToGraph( uri, model.getGraph() )   	
      	Logger.getRootLogger().info(s"storeURI uri $uri : stored")
      	model.getGraph
      } catch {
      case t: Throwable => Logger.getRootLogger().error( "ERROR: " + t )
      ModelFactory.createDefaultModel().getGraph
      }
    }).getOrElse( emptyGraph )
  }
    
  def storeURI(uri: Rdf#URI, graphUri: Rdf#URI, store: JenaStore ) //  RDFStore[Rdf] )
    : Rdf#Graph
    = {
    store.writeTransaction {
      Logger.getRootLogger().info(s"storeURI uri $uri graphUri $graphUri")
      try{
        val gForStore = store.getGraph(graphUri)
        val model = RDFDataMgr.loadModel(uri.toString())
        
        // not possible, as Banana also starts a transaction:
//        store.appendToGraph( uri, model.getGraph() )
        
        store.dg.addGraph( uri, model.getGraph() )
        
        //  model.getNsPrefixMap // TODO use it to load referred vocab's
        Logger.getRootLogger().info(s"storeURI uri $uri : stored")
        model.getGraph
      } catch {
      case t: Throwable => Logger.getRootLogger().error( "ERROR: " + t )
      ModelFactory.createDefaultModel().getGraph
      }
    }
  }
}