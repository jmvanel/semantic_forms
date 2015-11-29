package deductions.runtime.html

import org.w3.banana.RDF
import deductions.runtime.abstract_syntax.FormSyntaxFactoryTest
import org.w3.banana.io.RDFReader
import org.w3.banana.io.Turtle
import org.w3.banana.RDFOps
import org.w3.banana.jena.Jena
import org.junit.Assert
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import org.w3.banana.jena.JenaModule
import org.scalatest.FunSuite
import com.hp.hpl.jena.query.Dataset
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import org.apache.log4j.Logger

//class Form2HTMLTestJena extends Form2HTMLTest

class Form2HTMLTest
    extends FunSuite
    with RDFStoreLocalJena1Provider //    with JenaModule
    with FormSyntaxFactoryTest[Jena, Dataset] {

  val logger = Logger.getRootLogger()

  import ops._
  println("Entering Form2HTMLTest")
  val nullURI1 = makeUri("")

  test("Test HTML") {
    val fh = new Form2HTML[Rdf#Node, Rdf#URI] {
      //      override 
      val nullURI = nullURI1 // Ops.makeURI("")
    }
    val xhtml = fh.generateHTML(createFormWithGivenProps())
    System.out.println(xhtml)
    Files.write(Paths.get("tmp.form.html"), xhtml.toString().getBytes);
    Assert.assertTrue(xhtml.toString().contains("Alexandre"))
  }
}
