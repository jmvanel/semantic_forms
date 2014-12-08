package deductions.runtime.services

import org.scalatest.FunSuite
import org.w3.banana.RDFOpsModule
import org.w3.banana.SparqlGraphModule
import org.w3.banana.SparqlOpsModule
import deductions.runtime.jena.RDFStoreObject
import deductions.runtime.sparql_cache.RDFCacheJena
import deductions.runtime.jena.RDFStoreLocalProvider
import org.w3.banana.SparqlHttpModule
import org.w3.banana.RDFOps
import org.w3.banana.io.RDFReader
import org.w3.banana.io.RDFWriter
import org.w3.banana.RDFStore
import org.w3.banana.io.Turtle
import org.w3.banana.io.RDFXML
import scala.util.Try
import org.w3.banana.RDF
import org.w3.banana.SparqlOps

class TestBrowsableGraph [Rdf <: RDF, Store]
  extends FunSuite
//  (implicit
//  ops: RDFOps[Rdf],
//  turtleReader: RDFReader[Rdf, Try, Turtle],
//  turtleWriter : RDFWriter[Rdf, Try, Turtle],
//  rdfXMLWriter: RDFWriter[Rdf, Try, RDFXML],
//  rdfStore: RDFStore[Rdf, Try, Store],
//  sparqlOps: SparqlOps[Rdf]
//)
  with RDFCacheJena
   with RDFOpsModule
   with SparqlGraphModule
   with SparqlOpsModule
   with SparqlHttpModule
   with RDFStoreLocalProvider
{
  import ops._
  
  def test {
//  lazy val store =  RDFStoreObject.store
  lazy val bg = new BrowsableGraph()
  
  val uri = "http://jmvanel.free.fr/jmv.rdf#me"
  retrieveURI(makeUri(uri), dataset)
  println( "bg.focusOnURI(uri)\n" + bg.focusOnURI(uri) )
}
}