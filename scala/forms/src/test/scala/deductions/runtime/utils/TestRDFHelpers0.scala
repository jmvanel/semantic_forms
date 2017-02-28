package deductions.runtime.utils

import org.scalatest.FunSuite

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.DefaultConfiguration

class TestRDFHelpers0
    extends FunSuite
    with ImplementationSettings.RDFModule
    with RDFHelpers0[ImplementationSettings.Rdf]
    with RDFPrefixes[ImplementationSettings.Rdf] {

  import ops._
  val config = new DefaultConfiguration{}
  
  test("compare") {
    val uri = URI("http://jmvanel.free.fr/jmv.rdf#me")
    val tr1 = Triple(uri, foaf.name, Literal("bla") )
    assert( compareTriples(tr1, tr1), "tr1 == tr1" )
  }
}
