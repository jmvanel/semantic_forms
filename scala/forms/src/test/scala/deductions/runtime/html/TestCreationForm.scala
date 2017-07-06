package deductions.runtime.html

import java.io.PrintStream
import java.nio.file.{Files, Paths}

import deductions.runtime.abstract_syntax.InstanceLabelsInferenceMemory
import deductions.runtime.jena.{ImplementationSettings, RDFStoreLocalJena1Provider}
import deductions.runtime.sparql_cache.SPARQLHelpers
import deductions.runtime.utils.{Configuration, DefaultConfiguration}
import org.apache.log4j.Logger
import org.scalatest.{BeforeAndAfter, FunSuite}
import org.w3.banana.{FOAFPrefix, OWLPrefix, RDF, RDFOps, RDFOpsModule, RDFSPrefix, TurtleWriterModule}

import scala.collection.JavaConversions._
import scala.xml.{Elem, NodeSeq}

/**
 * Test Creation Form from class URI, without form specification
 * NOTE: the TDB database is used here
 */
class TestCreationForm extends {
  override val config = new DefaultConfiguration{}
}   with FunSuite
    with ImplementationSettings.RDFModule
    with CreationFormAlgo[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with GraphTestEnum[ImplementationSettings.Rdf]
    with RDFStoreLocalJena1Provider
    with SPARQLHelpers[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with InstanceLabelsInferenceMemory[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with BeforeAndAfter
    with Configuration
    with DefaultConfiguration {

	// PASTED code:
//	val conf = config
//  val ops1 = ops
//  val htmlGenerator = new Form2HTMLBanana[ImplementationSettings.Rdf] {
//    implicit val ops = ops1
//    val config = conf
//    val nullURI = ops.URI("")
//  }

	override lazy val htmlGenerator =
    Form2HTMLObject.makeDefaultForm2HTML(config)(ops)
    
  override val lookup_domain_unionOf = true
  val logger = Logger.getRootLogger()
  import ops._

  /* CAUTION: BeforeAndAfterAll.afterAll DOES NOT WORK directly with FunSuite:
 * afterAll runs after EACH test */
  //  override def afterAll {
  //    FileUtils.deleteLocalSPARQL()
  //  }

  before {
    println("!!!!!!!!!!!!!!!!!!!!!! before")
  }
  after {
    println("!!!!!!!!!!!!!!!!!!!!!! after")
    rdfStore.rw(dataset, {
      dataset.removeNamedModel(foaf.prefixIri)
      dataset.removeNamedModel("Person")
      // NOTE: this named graph was added in  TestCreationForm2 :
      dataset.removeNamedModel("test")
    })
    println("""dataset.listNames().mkString("\n")""")
    rdfStore.rw(dataset, {
      println(dataset.listNames().mkString("\n"))
    }).get
  }

  test("display form from class") {
    val classUri = foaf.Person
    println("retrieveURI(URI(foaf.prefixIri)\n" +
      retrieveURI(URI(foaf.prefixIri), dataset)
    )
    implicit val graph =
      rdfStore.rw(dataset, { allNamedGraph }).get
    val rawForm = createElem(classUri.toString(), lang = "fr")
    TestCreationForm.printWrapedWithHTML(rawForm, "example.creation.form.html")

    val c1 = rawForm.toString().contains("topic_interest")
    assert(c1)
    val c2 = rawForm.toString().contains("firstName")
    assert(c2)
    //    assert(rawForm.toString().contains("knows"))
  }

  test("display form from class with instance for possible values") {
    val classUri = foaf.Person
    // "http://usefulinc.com/ns/doap#Project"
    //       foaf.Organization
    retrieveURI(URI(foaf.prefixIri), dataset)
    // to test possible values generation with foaf:knows :
    val subjectURI = URI("http://jmvanel.free.fr/jmv.rdf#me")
    retrieveURI(subjectURI, dataset)
    val lang = "fr"
    rdfStore.rw(dataset, {
      replaceInstanceLabel(subjectURI, allNamedGraph, lang)
    })
    val rawForm = create(classUri.toString(), lang = lang).get
    //    val rawForm = createElem(classUri.toString(), lang = "fr")
    //    val rawForm = createFormFromClass(classUri )
    val form = TestCreationForm.wrapWithHTML(rawForm)
    TestCreationForm.printWrapedWithHTML(rawForm, "/tmp/example.creation.form.html")
    assert(rawForm.toString().contains("topic_interest"))
    assert(rawForm.toString().contains("firstName"))
    assert(rawForm.toString().contains("knows"))
    // included in pull-down menu for knows
    assert(rawForm.toString().contains("Jean-Marc"))
    // NOTE: homepage is not present, because it has rdfs:domain owl:Thing
  }

  test("display form from vocab' with owl:oneOf") {
    import ops._
    implicit val graph =
      rdfStore.rw(dataset, {
        rdfStore.appendToGraph(dataset, URI("Person"), vocab)
        allNamedGraph
      }).get
    //    implicit val graph = rdfStore.rw(dataset, { allNamedGraph }).get
    //    val rawForm = createElem("Person", lang = "fr")
    val rawForm = create("Person", lang = "fr").get
    val form = TestCreationForm.wrapWithHTML(rawForm)
    val file = "example.creation.form2.html"
    Files.write(Paths.get(file), form.toString().getBytes);
    println(s"file created $file")

    assert(rawForm.toString().contains("style"))
    assert(rawForm.toString().contains("evil"))
  }

}

trait GraphTestEnum[Rdf <: RDF ] extends RDFOpsModule with TurtleWriterModule
//with DefaultConfiguration
//    with RDFPrefixes[Rdf]
{
  implicit val ops: RDFOps[Rdf]

  import ops._
  private val foaf = FOAFPrefix[Rdf]
  private val owl = OWLPrefix[Rdf]
  private val rdfs = RDFSPrefix[Rdf]

  val vocab1 = (
    URI("PersonType")
    -- rdf.typ ->- owl.Class
    -- owl.oneOf ->- List(URI("hero"), URI("evil"), URI("wise"))
  ).graph

  val alexs = Seq(
    bnode("a") -- foaf.name ->- "Alexandre".lang("fr"),
    bnode("b") -- foaf.name ->- "Alexander".lang("en")
  )

  val vocab11 = (
    URI("WorkType")
    -- rdf.typ ->- owl.Class
    -- owl.oneOf ->- (BNode("Dilbert") -- rdfs.label ->- "")
    -- owl.oneOf ->- List(
      (BNode("DilbertBN") -- rdfs.label ->- "Dilbert"),
      (BNode("AliceBN") -- rdfs.label ->- "Alice"),
      (BNode("WallyBN") -- rdfs.label ->- "Wally")
    )
  ).graph

  val vocab11bn = (
    BNode("WorkTypeBN")
    -- rdf.typ ->- owl.Class
    -- owl.oneOf ->- (BNode("Dilbert") -- rdfs.label ->- "")
    -- owl.oneOf ->- List(
      (BNode("DilbertBN") -- rdfs.label ->- "Dilbert"),
      (BNode("AliceBN") -- rdfs.label ->- "Alice"),
      (BNode("WallyBN") -- rdfs.label ->- "Wally")
    )
  ).graph

  val vocab2 = (
    URI("Person") -- rdf.typ ->- owl.Class).graph
  val vocab3 = (
    URI("style")
    -- rdf.typ ->- owl.ObjectProperty
    -- rdfs.domain ->- URI("Person")
    -- rdfs.range ->- URI("PersonType")
    -- rdfs.label ->- "style de personne"
  ).graph
  val vocab31 = (
    URI("workStyle")
    -- rdf.typ ->- owl.ObjectProperty
    -- rdfs.domain ->- URI("Person")
    -- rdfs.range ->- URI("WorkType")
    -- rdfs.label ->- "style de travail"
  ).graph
  val vocab31bn = (
    URI("workStyleBN")
    -- rdf.typ ->- owl.ObjectProperty
    -- rdfs.domain ->- URI("Person")
    -- rdfs.range ->- BNode("WorkTypeBN")
    -- rdfs.label ->- "style de travail"
  ).graph
  val vocab = vocab1 union vocab11 union vocab11bn union vocab2 union vocab3 /* union vocab31 */ union vocab31bn

  println("==== vocab ====")
  val os = new PrintStream(System.out)
  turtleWriter.write(vocab, os, "")
}

object TestCreationForm {

  //  printWrapedWithHTML(rawForm, "example.creation.form.html")
  def printWrapedWithHTML(rawForm: NodeSeq, file: String) = {
    val form = TestCreationForm.wrapWithHTML(rawForm)
    Files.write(Paths.get(file), form.toString().getBytes);
    println(s"file created $file")
  }

  def wrapWithHTML(e: NodeSeq): Elem =
    <html>
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8"></meta>
        <style type="text/css">
          .resize {{ resize: both; width: 100%; height: 100%; }}
          .overflow {{ overflow: auto; width: 100%; height: 100%; }}
					.form-control {{ width:100% }}
        </style>
      </head>
      <body>{ e }</body>
    </html>

}
