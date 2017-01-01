package deductions.runtime.abstract_syntax

import java.io.FileInputStream
import java.io.FileOutputStream

import org.apache.log4j.Logger
import org.hamcrest.BaseMatcher
import org.junit.Assert
import org.scalatest.FunSuite
import org.w3.banana.RDF

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.services.DefaultConfiguration

class FormSyntaxFactoryTestJena extends FunSuite
    with RDFStoreLocalJena1Provider
    with FormSyntaxFactoryTest[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with DefaultConfiguration {
  val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
  val logger = Logger.getRootLogger()

  test("form contains label and data") {
    val form = createFormWithGivenProps
    println("form:\n" + form)
    Assert.assertThat("form contains label and data", form.toString,
      new BaseMatcher[String]() {
        def matches(a: Any): Boolean = {
          val s = a.toString
          s.contains("Alexandre") &&
            s.contains("name") &&
            s.contains("Henry Story")
        }
        def describeTo(x$1: org.hamcrest.Description): Unit = {}
      })
  }

  test("form With inferred fields") {
    val form = createFormWithInferredProps()
    println("form:\n" + form)
    Assert.assertThat("form contains label and data", form.toString,
      new BaseMatcher[String]() {
        def matches(a: Any): Boolean = {
          val s = a.toString
          s.contains("Alexandre") &&
            s.contains("name") &&
            s.contains("Henry Story")
        }
        def describeTo(x$1: org.hamcrest.Description): Unit = {}
      })
  }

  test("form from Class") {
    val form = createFormFromClass
    println("form:\n" + form)
  }

}

///////////////////////

/** NOTE: the TDB database is not used here, 
 * the data and vocab' are passed by:
 * implicit val graph */
trait FormSyntaxFactoryTest[Rdf <: RDF, DATASET] extends FormSyntaxFactory[Rdf, DATASET] {
  import ops._
  import rdfStore.transactorSyntax._

  //  import FormSyntaxFactory._
//  private lazy val foaf = FOAFPrefix[Rdf]

  def makeFOAFsample: Rdf#Graph = {
    (URI("betehess")
      -- foaf.name ->- "Alexandre".lang("fr")
      -- foaf.title ->- "Mr"
      -- rdf.typ ->- foaf.Person 
      -- foaf.knows ->- (
        URI("http://bblfish.net/#hjs")
        -- foaf.name ->- "Henry Story"
        -- foaf.currentProject ->- URI("http://webid.info/"))).graph
  }

  def makeGraphwithFOAFvocabandData() = {
		  val graph1 = makeFOAFsample
		  val resource = new FileInputStream("src/test/resources/foaf.n3")
		  val graph2 = turtleReader.read(resource, foaf.prefixIri).get
		  union(Seq(graph1, graph2))
  }
  
  def createFormWithGivenProps() = {
    implicit val graph = // emptyGraph // 
      makeGraphwithFOAFvocabandData()    
    val factory = this
    println((graph.triples).mkString("\n"))
    val res = dataset.r({
      val form = factory.createFormDetailed(
        URI("betehess"),
        Seq(foaf.title,
          foaf.name, foaf.knows),
        URI(""),
        EditionMode)
      form
    })
    res.get
  }

  def createFormWithInferredProps() = {    
    val factory = this
    val res = dataset.r({
    	implicit val graph = makeGraphwithFOAFvocabandData()
      factory.createForm(URI("betehess"), editable = true)
    })
    res.get
  }

  def createFormFromClass() = {
    val resource = new FileInputStream("src/test/resources/foaf.n3")
    val graph2 = turtleReader.read(resource, foaf.prefixIri).get
    val formspec = new FileInputStream("form_specs/foaf.form.ttl")
    val graph1 = turtleReader.read(formspec, "").get
    implicit val graph = union(Seq(graph1, graph2))
    //  val fact = new UnfilledFormFactory[Rdf](graph)
    val fact = this // new FormSyntaxFactory[Rdf](graph)
    val os = new FileOutputStream("/tmp/graph.nt")
    turtleWriter.write(graph, os, "")
    val res = dataset.r({
      fact.createFormDetailed(URI("betehess"), Seq(foaf.topic_interest), foaf.Person,
        DisplayMode)
    })
  }

}