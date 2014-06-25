
import org.w3.banana._
import org.w3.banana.jena.JenaModule
import deductions.runtime.abstract_syntax.FormSyntaxFactory
import org.w3.banana.diesel._
import org.w3.banana.jena.Jena

trait TestBananaDSL
  extends RDFModule
  with RDFOpsModule
//  with TurtleReaderModule
//  with RDFXMLWriterModule
//
//  with JenaModule // <<<< compile pas si on active ça >>>>
{
  import Ops._
  
  val foaf = FOAFPrefix[Rdf]
  val foafURI = foaf.prefixIri
  val exampleGraph = (
    URI("betehess")
    -- foaf.name ->- "Alexandre".lang("fr")
    -- foaf.title ->- "Mr"
    -- foaf.knows ->- (
      URI("http://bblfish.net/#hjs")
      -- foaf.name ->- "Henry Story"
      -- foaf.currentProject ->- URI("http://webid.info/"))).graph
}