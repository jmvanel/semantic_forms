package deductions.runtime.semlogs

import org.junit.Assert
import org.scalatest.FunSuite
import org.w3.banana.RDF
import org.w3.banana.XSDPrefix

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.services.DefaultConfiguration

trait TimeSeriesTest[Rdf <: RDF, DATASET] extends FunSuite with TimeSeries[Rdf, DATASET] {
  
  import ops._
  val xsd = XSDPrefix[Rdf]
  val label = "my label"
  
  val tLabel = Triple(URI("a"), rdfs.label, Literal(label) )
  val predURI = URI("urn:p")
  
  val addedTriples1 = Seq( Triple(URI("a"), predURI, Literal("1", xsd.double )), tLabel)
  val addedTriples2 = Seq( Triple(URI("a"), predURI, Literal("2", xsd.double )), tLabel)
      
  test("notifyDataEvent + getTimeSeries") {
    implicit val userURI = "urn:jmv1"
    notifyDataEvent(addedTriples1, /*removedTriples*/ Seq() )
    Thread.sleep( 200 )
    notifyDataEvent(addedTriples2, /*removedTriples*/ Seq() )
    val results = getTimeSeries( fromUri(predURI) )
    println( "results " + results )
    val resPair = results.getOrElse(label, Seq() ).head
    Assert.assertTrue( 
    		( resPair._2 == 1.0 )   || 
        ( resPair._2 == 2.0 ) )
  }
}

class TimeSeriesTestJena extends FunSuite
    with RDFStoreLocalJena1Provider
    with TimeSeriesTest[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
}