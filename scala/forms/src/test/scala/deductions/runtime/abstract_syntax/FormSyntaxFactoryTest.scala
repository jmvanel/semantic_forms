package deductions.runtime.abstract_syntax

import java.io.{FileInputStream, FileOutputStream}

import deductions.runtime.jena.{ImplementationSettings, RDFStoreLocalJenaProvider}
import deductions.runtime.utils.DefaultConfiguration
//import org.apache.log4j.Logger
import org.hamcrest.BaseMatcher
import org.junit.Assert
import org.scalatest.FunSuite
import org.w3.banana.RDF
import org.scalatest.BeforeAndAfterAll
import scala.xml.Source
import deductions.runtime.TestsBase
import deductions.runtime.core.HTTPrequest
import com.typesafe.scalalogging.Logger
import org.scalatest.funsuite.AnyFunSuite
import deductions.runtime.utils.FormModuleBanana

class FormSyntaxFactoryTestJena extends AnyFunSuite
    with RDFStoreLocalJenaProvider
    with FormSyntaxFactoryTest[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with DefaultConfiguration
    with FormModuleBanana[ImplementationSettings.Rdf]
    with BeforeAndAfterAll {
  val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
//  val logger = Logger("test") // .getRootLogger()

  override def stringToAbstractURI(uri: String): deductions.runtime.jena.ImplementationSettings.Rdf#URI = ???
  override def toPlainString(n: deductions.runtime.jena.ImplementationSettings.Rdf#Node): String = ???

  test("form contains label and data") {
    val form = createFormWithGivenProps
    println("form:\n" + form.toString().substring(0, 5000))
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
    val s = form.toString()
    println("form:\n" + s.substring(0, Math.min(5000, s.length)))
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

  override def afterAll {
    close()
  }

}

///////////////////////

/** Test FormSyntaxFactory with fake data; TODO : should be in module abstract_syntax
 *
 *  NOTE: the TDB database is not used here,
 * the data and vocab' are passed by:
 * implicit val graph */
trait FormSyntaxFactoryTest[Rdf <: RDF, DATASET] extends FormSyntaxFactory[Rdf, DATASET]
with TestsBase {
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
    val resource = new FileInputStream( relativeTestDir + "/src/test/resources/foaf.n3")
      // getClass.getResourceAsStream("foaf.n3")

    println(">>>> makeGraphwithFOAFvocabandData")
    // println( Source.fromInputStream(resource).getByteStream )

    val graph2 = turtleReader.read(resource, foaf.prefixIri).get
		  union(Seq(graph1, graph2))
  }
  
  def createFormWithGivenProps()
  //: FormSyntax
  = {
    implicit val graph = makeGraphwithFOAFvocabandData()
    implicit val lang = "en"
    val factory = this
    println((graph.triples).take(10).mkString("\n"))
    val res = dataset.r({
      val form = factory.createFormDetailed(
        URI("betehess"),
//        Seq(foaf.title,
//          foaf.name, foaf.knows),
        URI(""),
        EditionMode, nullURI, nullURI,
        HTTPrequest())
      form
    })
    res.get
  }

  def createFormWithInferredProps() = {    
    val factory = this
//    val res = dataset.r({
    	implicit val graph = makeGraphwithFOAFvocabandData()
      factory.createFormTR(URI("betehess"), editable = true)
//    })
//    res.get
  }

  def createFormFromClass() = {
    val resource = new FileInputStream(relativeTestDir + "/src/test/resources/foaf.n3")
    val graph2 = turtleReader.read(resource, foaf.prefixIri).get
    val formspec = new FileInputStream( relativeTestDir + "/form_specs/foaf.form.ttl")
    val graph1 = turtleReader.read(formspec, "").get
    implicit val graph = union(Seq(graph1, graph2))
    implicit val lang = "en"
    //  val fact = new UnfilledFormFactory[Rdf](graph)
    val fact = this // new FormSyntaxFactory[Rdf](graph)
    val os = new FileOutputStream("/tmp/graph.nt")
    turtleWriter.write(graph, os, "")
    val res = dataset.r({
      fact.createFormDetailed(URI("betehess"),
//          Seq(foaf.topic_interest), 
          foaf.Person,
        DisplayMode, nullURI, nullURI,
        HTTPrequest())
    })
  }

}
