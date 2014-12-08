import org.w3.banana.FOAFPrefix
import org.w3.banana.RDFModule
import org.w3.banana.RDFOpsModule
import org.w3.banana.diesel._
import org.w3.banana.jena.JenaModule


object TestBananaDSLApp extends App 
		with TestBananaDSL
		with JenaModule {
  println( exampleGraph.toString )
}

trait TestBananaDSL
  extends RDFModule
  with RDFOpsModule
{
  import ops._
  
  lazy val foaf = FOAFPrefix[Rdf]
//  val foafURI = foaf.prefixIri
  lazy val exampleGraph = (
    URI("betehess")
    -- foaf.name ->- "Alexandre".lang("fr")
    -- foaf.title ->- "Mr"
    -- foaf.knows ->- (
      URI("http://bblfish.net/#hjs")
      -- foaf.name ->- "Henry Story"
      -- foaf.currentProject ->- URI("http://webid.info/"))).graph
}