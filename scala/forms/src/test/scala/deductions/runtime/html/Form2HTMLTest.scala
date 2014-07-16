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
class Form2HTMLTestJena extends Form2HTMLTest

class Form2HTMLTest
      extends FunSuite with FormSyntaxFactoryTest
      with JenaModule {
  import Ops._
  println("Entering Form2HTMLTest")
  val nullURI1 = makeUri("")

  test("Test HTML") {
    val fh = new Form2HTML[Rdf#URI]{
      override val nullURI = nullURI1 // Ops.makeURI("")
    }
    val xhtml = fh.generateHTML(createForm)
    System.out.println(xhtml)
    Files.write(Paths.get("tmp.form.xhtml"), xhtml.toString().getBytes );
    Assert.assertTrue(xhtml.toString().contains("Alexandre"))
  }
}
