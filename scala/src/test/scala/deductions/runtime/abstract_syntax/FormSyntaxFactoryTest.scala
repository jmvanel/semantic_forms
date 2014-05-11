package deductions.runtime.swing.components.abstract_syntax

import java.io.FileInputStream

import org.w3.banana.FOAFPrefix
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFReader
import org.w3.banana.Turtle
import org.w3.banana.diesel.toPointedGraphW
import org.w3.banana.jena.Jena

import deductions.runtime.abstract_syntax.FormSyntaxFactory

object FormSyntaxFactoryTestApp extends FormSyntaxFactoryTest[Jena] with App

class FormSyntaxFactoryTest[Rdf <: RDF]
  ( implicit ops: RDFOps[Rdf],
    turtleReader : RDFReader[Rdf, Turtle] )
{
    import ops._
    val foaf = FOAFPrefix[Rdf]
    val graph1 = (
      URI("betehess")
      -- foaf.name ->- "Alexandre".lang("fr")
      -- foaf.title ->- "Mr"
      -- foaf.knows ->- (
        URI("http://bblfish.net/#hjs")
        -- foaf.name ->- "Henry Story"
        -- foaf.currentProject ->- URI("http://webid.info/"))) . graph
  
    val resource = new FileInputStream("src/test/resources/foaf.n3" )
    val graph2 = turtleReader.read(resource, "http://xmlns.com/foaf/0.1/" ) . get

//    val graph = graph1.union(graph2)	// KO !
//    val graph = union (graph1 :: graph2)	// KO !
    val graph = union ( Seq(graph1, graph2.asInstanceOf[Rdf#Graph]))
    
    val fact = new FormSyntaxFactory[Rdf](graph)
    println( ( graph.toIterable ) . mkString("\n") )
    val form = fact.createForm( URI("betehess"),
        Seq( foaf.name ) )
    println( "form:\n" + form )
}