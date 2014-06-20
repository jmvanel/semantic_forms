package deductions.runtime.html

import org.w3.banana.RDF
import deductions.runtime.swing.components.abstract_syntax.FormSyntaxFactoryTest
import org.w3.banana.RDFReader
import org.w3.banana.Turtle
import org.w3.banana.RDFOps
//import deductions.runtime.html.Form2HTML
import org.w3.banana.jena.Jena

object Form2HTMLTestApp extends Form2HTMLTest[Jena] with App

class Form2HTMLTest[Rdf <: RDF] 
  ( implicit ops: RDFOps[Rdf],
      turtleReader : RDFReader[Rdf, Turtle] )
      extends FormSyntaxFactoryTest[Rdf]
      with Form2HTML[Rdf#URI]
{
  println("Entering Form2HTMLTest")
  val nullURI = ops.makeUri("")
   println( generateHTML( form ) )
}
