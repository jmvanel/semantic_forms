package deductions.runtime.jena

import org.w3.banana.jena.Jena
import org.apache.log4j.Logger
import org.apache.jena.riot.RDFDataMgr
import org.w3.banana.RDFOpsModule
import org.w3.banana.SparqlOps
import java.net.URL
import org.w3.banana.jena.JenaModule
import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.rdf.model.ModelFactory
import org.w3.banana.RDFStore
import org.w3.banana.diesel._
import org.w3.banana.RDFStore
import org.w3.banana.RDFOpsModule
import org.w3.banana.RDFOps
import scala.util.Try
import org.w3.banana.RDF

/** Helpers for RDF Store
 *  paved the way for Banana 0.7 :
 *  - JenaStore is not existing anymore
 *  - generic API for transactions 
 * TODO rename RDFStoreHelpers  */

trait JenaHelpers /*[Rdf]*/ extends 
// RDFStore[Rdf, Try, RDFStoreObject.DATASET ]
// with 
 JenaModule
// class StoreHelpers[Rdf <: RDF,
//   Store]
// (implicit
//  ops: RDFOps[Rdf],
////  turtleReader: RDFReader[Rdf, Try, Turtle],
////  rdfXMLWriter: RDFWriter[Rdf, Try, RDFXML],
//  rdfStore: RDFStore[Rdf, Try, Store]
//)
{
	import ops._

  type Store = RDFStoreObject.DATASET
  
    /** store URI in a named graph, using Jena's RDFDataMgr
     * (use content-type or else file extension) 
     * with Jena Riot for smart reading of any format,
     * cf https://github.com/w3c/banana-rdf/issues/105
    */
//    def storeURI(uri: Rdf#URI, graphUri: Rdf#URI, dataset : RDFStoreObject.DATASET ) //  RDFStore[Rdf] )
  def storeURI(uri: Rdf#URI, graphUri: Rdf#URI, dataset : Store )
    : Rdf#Graph = {
    rdfStore.rw( dataset, {      
      Logger.getRootLogger().info(s"storeURI uri $uri graphUri $graphUri")
      try{
//        val dataset =  RDFStoreObject.dataset
      	val gForStore = rdfStore.getGraph(dataset, graphUri)
        // read from uri no matter what the syntax is:
      	val model = RDFDataMgr.loadModel(uri.toString())
      	
      	rdfStore. appendToGraph( dataset, uri, model.getGraph() )   	
      	Logger.getRootLogger().info(s"storeURI uri $uri : stored")
      	model.getGraph
      } catch {
      case t: Throwable => Logger.getRootLogger().error( "ERROR: " + t )
      ModelFactory.createDefaultModel().getGraph
      }
    }).getOrElse( emptyGraph )
  }
}