package deductions.runtime.html
import org.scalatest.FunSuite
import deductions.runtime.jena.RDFStoreObject
import org.w3.banana.Prefix
import org.w3.banana.GraphStore
import org.w3.banana.RDF
import org.w3.banana._
import org.w3.banana.syntax._
import org.w3.banana.diesel._
import deductions.runtime.utils.FileUtils
import java.nio.file.Files
import java.nio.file.Paths
import scala.xml.Elem
import deductions.runtime.jena.RDFGraphPrinter
import java.io.PrintStream

/** Test Creation Form from class URI, without form specification */
class TestCreationForm extends FunSuite with CreationForm with GraphTestEnum {

  test("display form from instance") {
    FileUtils.deleteLocalSPARL()
    val classUri = // "http://usefulinc.com/ns/doap#Project"
      //       foaf.Organization
      foaf.Person
    retrieveURI(classUri, dataset)
    // to test possible values generation:
    retrieveURI(ops.makeUri("http://jmvanel.free.fr/jmv.rdf#me"), dataset)

    val rawForm = createElem(classUri.toString(), lang = "fr")
    val form = TestCreationForm.wrapWithHTML(rawForm)
    val file = "example.creation.form.html"
    Files.write(Paths.get(file), form.toString().getBytes);
    println(s"file created $file")

    assert(rawForm.toString().contains("homepage"))
  }

  test("display form from vocab' with owl:oneOf") {
    FileUtils.deleteLocalSPARL()
    import ops._
    rdfStore.rw(dataset, {
      rdfStore.appendToGraph(dataset, URI("Person"), vocab)
    })
    val rawForm = createElem(("Person"), lang = "fr")
    val form = TestCreationForm.wrapWithHTML(rawForm)
    val file = "example.creation.form2.html"
    Files.write(Paths.get(file), form.toString().getBytes);
    println(s"file created $file")

    assert(rawForm.toString().contains("style"))
    assert(rawForm.toString().contains("evil"))
    assert(rawForm.toString().contains("Dilbert"))
  }
}

trait GraphTestEnum extends RDFOpsModule with TurtleWriterModule {
  import ops._
  import syntax._
  val foaf = FOAFPrefix[Rdf]
  private val owl = OWLPrefix[Rdf]
  val rdfs = RDFSPrefix[Rdf]

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
  def wrapWithHTML(e: Elem): Elem =
    <html>
      <head>
        <style type="text/css">
          .resize {{ resize: both; width: 100%; height: 100%; }}
          .overflow {{ overflow: auto; width: 100%; height: 100%; }}
					.form-control {{ width:100% }}
        </style>
      </head>
      <body>{ e }</body>
    </html>

}
