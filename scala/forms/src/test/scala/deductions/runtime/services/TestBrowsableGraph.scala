package deductions.runtime.services

import org.scalatest.FunSuite
import org.w3.banana.RDFOpsModule
import org.w3.banana.SparqlGraphModule
import org.w3.banana.SparqlOpsModule

import deductions.runtime.jena.RDFStoreObject
import deductions.runtime.sparql_cache.RDFCacheJena

class TestBrowsableGraph 
  extends FunSuite with RDFCacheJena // TODO depend on generic Rdf
   with RDFOpsModule
   with SparqlGraphModule
   with SparqlOpsModule
{
  import Ops._
  
//  lazy val store =  RDFStoreObject.store
  lazy val bg = new BrowsableGraph(store)
  
  val uri = "http://jmvanel.free.fr/jmv.rdf#me"
  retrieveURI(makeUri(uri), store)
  println( "bg.focusOnURI(uri)\n" + bg.focusOnURI(uri) )
}