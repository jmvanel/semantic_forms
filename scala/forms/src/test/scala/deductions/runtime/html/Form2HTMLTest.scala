package deductions.runtime.html

import org.w3.banana.RDF
import deductions.runtime.abstract_syntax.FormSyntaxFactoryTest
import org.w3.banana.RDFReader
import org.w3.banana.Turtle
import org.w3.banana.RDFOps
import org.w3.banana.jena.Jena
import org.junit.Assert
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import org.w3.banana.jena.JenaModule
import org.scalatest.FunSuite

/** will only run in eclipse, not stb, as sbt will not run App's in src/test */
//object Form2HTMLTestApp extends Form2HTMLTest[Jena] with App { tHTML }
//object Form2HTMLTestApp extends Form2HTMLTest with App { tHTML }

class Form2HTMLTestJena extends Form2HTMLTest // [Jena]

class Form2HTMLTest // [Rdf <: RDF]
//  ( implicit ops: RDFOps[Rdf],
//      turtleReader : RDFReader[Rdf, Turtle] )
      extends FunSuite with FormSyntaxFactoryTest // [Rdf]
      with JenaModule
//      with Form2HTML[Rdf#URI]
{
  import Ops._
  println("Entering Form2HTMLTest")
  val nullURI1 = makeUri("")

  test("Test HTML") {
    val fh = new Form2HTML[Rdf#URI]{
      override val nullURI = nullURI1 // Ops.makeURI("")
      }

    val xhtml = fh.generateHTML(createForm)
    System.out.println(xhtml)
    // scala.tools.nsc.io.File("tmp.form.xhtml").writeAll(xhtml.toString())
//    File("tmp.form.xhtml").writeAll(xhtml.toString() )
    Files.write(Paths.get("tmp.form.xhtml"), xhtml.toString().getBytes );

    Assert.assertTrue(xhtml.toString().contains("Alexandre"))
  }
}
