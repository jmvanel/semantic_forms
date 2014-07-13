package deductions.runtime.sparql_cache

import org.w3.banana.RDFModule
import org.w3.banana.RDFOpsModule
import org.w3.banana.RDFXMLReaderModule
import org.w3.banana.TurtleReaderModule
import org.w3.banana.jena.JenaStore

import deductions.runtime.jena.JenaHelpers

/** */
trait RDFCache
  extends RDFModule
  with RDFOpsModule
  with TurtleReaderModule
  with RDFXMLReaderModule {
}

trait RDFCacheJena extends RDFCache with JenaHelpers {
  //   self =>

  /**
   * retrieve URI from a graph named by itself;
   * according to timestamp retrieve from Jena Store,
   * or download
   */
  def retrieveURI(uri: Rdf#URI, store: JenaStore): Rdf#Graph = {
    store.getGraph(uri)
    // TODO according to timestamp, retrieve or download
  }

  /**
   * store URI in a graph named by itself,
   *  and stores the timestamp TODO
   */
  def storeURI(uri: Rdf#URI, store: JenaStore) {
    storeURI(uri, uri, store)
//    store.appendToGraph(uri, triples)
  }

}
