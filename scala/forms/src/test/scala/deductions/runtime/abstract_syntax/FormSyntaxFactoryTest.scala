package deductions.runtime.abstract_syntax

import java.io.FileInputStream

import org.hamcrest.BaseMatcher
import org.junit.Assert
import org.scalatest.FunSuite
import org.w3.banana.FOAFPrefix
import org.w3.banana.RDFModule
import org.w3.banana.RDFOpsModule
import org.w3.banana.TurtleReaderModule
import org.w3.banana.diesel._
import org.w3.banana.jena.JenaModule

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
    extends RDFOpsModule
    with TurtleReaderModule {

  def createForm() = {
    import ops._

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
    val graph2 = turtleReader.read(resource, "http://xmlns.com/foaf/0.1/").get
    val graph = union(Seq(graph1, graph2))

    val fact = new FormSyntaxFactory[Rdf](graph)
    println((graph.triples).mkString("\n"))
    val form = fact.createForm(
      URI("betehess"),
      Seq(foaf.title,
        foaf.name), URI(""))
    form
  }
}