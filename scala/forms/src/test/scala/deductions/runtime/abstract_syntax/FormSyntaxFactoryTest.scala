package deductions.runtime.abstract_syntax

import java.io.FileInputStream
import org.hamcrest.BaseMatcher
import org.junit.Assert
import org.junit.Test
import org.w3.banana.FOAFPrefix
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFReader
import org.w3.banana.Turtle
import org.w3.banana.diesel.toPointedGraphW
import org.w3.banana.jena.Jena
import org.scalatest.FunSuite
import deductions.runtime.abstract_syntax.FormSyntaxFactory
import org.w3.banana.RDFModule
import org.w3.banana.RDFOpsModule
import org.w3.banana.TurtleReaderModule
import org.w3.banana.RDFDSL
import org.w3.banana.jena.JenaModule


//trait RDFDSLModule extends RDFModule with RDFDSL[RDFModule.Rdf] {
////  implicit val Ops: RDFOps[Rdf]
//}

//object FormSyntaxFactoryTestApp extends FormSyntaxFactoryTestJena with App {t1}

//class FormSyntaxFactoryTestJena0 extends FormSyntaxFactoryTest[Jena]
class FormSyntaxFactoryTestJena extends FunSuite 
with JenaModule
with FormSyntaxFactoryTest // [Jena]
  {
  test("form contains label and data") {
    val form = createForm
    println("form:\n" + form)
    Assert.assertThat("form contains label and data", form.toString,
      new BaseMatcher[String]() {
        def matches(a: Any): Boolean = {
          val s = a.toString
          s.contains("Alexandre") &&
            s.contains("name")
        }
        def describeTo(x$1: org.hamcrest.Description): Unit = {}
      })
  }
}
trait FormSyntaxFactoryTest // [Rdf <: RDF]
extends RDFModule
with RDFOpsModule
with TurtleReaderModule
//with RDFDSLModule
//  ( implicit ops: RDFOps[Rdf],
//    turtleReader : RDFReader[Rdf, Turtle] 
//  )
  {

  def createForm() = {
  import Ops._

  val foaf = FOAFPrefix[Rdf]
  val graph1 = (
    URI("betehess")
    -- foaf.name ->- "Alexandre".lang("fr")
    -- foaf.title ->- "Mr"
    -- foaf.knows ->- (
      URI("http://bblfish.net/#hjs")
      -- foaf.name ->- "Henry Story"
      -- foaf.currentProject ->- URI("http://webid.info/"))).graph

  val resource = new FileInputStream("src/test/resources/foaf.n3")
  val graph2 = TurtleReader.read(resource, "http://xmlns.com/foaf/0.1/").get

  //    val graph = graph1.union(graph2)	// KO !
  //    val graph = union (graph1 :: graph2)	// KO !
  val graph = union(Seq(graph1, graph2.asInstanceOf[Rdf#Graph]))

  val fact = new FormSyntaxFactory[Rdf](graph)
  println((graph.toIterable).mkString("\n"))
  val form = fact.createForm(URI("betehess"),
    Seq(foaf.name))
  form
  }
}