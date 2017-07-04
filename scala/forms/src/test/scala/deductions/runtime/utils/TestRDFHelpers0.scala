package deductions.runtime.utils

import deductions.runtime.jena.ImplementationSettings
import org.scalatest.FunSuite

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
