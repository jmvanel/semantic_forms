package deductions.runtime.services

import java.net.URLEncoder

import org.w3.banana.RDFOpsModule

import deductions.runtime.jena.RDFStoreObject
import deductions.runtime.sparql_cache.RDFCacheJena

object TestFormSaver 
  extends App with RDFCacheJena // TODO depend on generic Rdf
   with RDFOpsModule
{
  import Ops._
  
  lazy val store =  RDFStoreObject.store
  lazy val fs = new FormSaver(store)
  
  val uri = "http://jmvanel.free.fr/jmv.rdf#me"
  storeURI(makeUri(uri), store)
  val map: Map[String, Seq[String]] = Map(
      "uri" ->  Seq( encode(uri) ),
      "url" ->  Seq( encode(uri) ),
      encode("LIT-http://xmlns.com/foaf/0.1/familyName") -> Seq("Van Eel"),
      encode("ORIG-LIT-http://xmlns.com/foaf/0.1/familyName") -> Seq("Vanel")
  )
  fs.saveTriples(map)
  // TODO assert that foaf:familyName "Vanel" has been removed from Triple store
  
  ////
  def encode(uri:String) = { URLEncoder.encode(uri, "utf-8" ) }
}