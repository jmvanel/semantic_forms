package deductions.runtime.html

import org.w3.banana.RDF
import deductions.runtime.components.abstract_syntax.FormSyntaxFactoryTest
import org.w3.banana.RDFReader
import org.w3.banana.Turtle
import org.w3.banana.RDFOps
import org.w3.banana.jena.Jena
import org.junit.Assert
import org.junit.Test

object Form2HTMLTestApp extends Form2HTMLTest[Jena] with App { tHTML }

class Form2HTMLTestJena extends Form2HTMLTest[Jena]

class Form2HTMLTest[Rdf <: RDF]
  ( implicit ops: RDFOps[Rdf],
      turtleReader : RDFReader[Rdf, Turtle] )
      extends FormSyntaxFactoryTest[Rdf]
      with Form2HTML[Rdf#URI]
{
  println("Entering Form2HTMLTest")
  val nullURI = ops.makeUri("")

  @Test def tHTML() {
    val xhtml = generateHTML(form)
    println(xhtml)
    Assert.assertTrue(xhtml.toString().contains("Alexandre"))
  }
}
