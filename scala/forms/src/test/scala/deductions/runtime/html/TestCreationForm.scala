package deductions.runtime.html
import org.scalatest.FunSuite
import deductions.runtime.jena.RDFStoreObject
import org.w3.banana.Prefix
import org.w3.banana.GraphStore
import org.w3.banana.RDF
import org.w3.banana._
import org.w3.banana.syntax._
import org.w3.banana.diesel._
import deductions.runtime.utils.Fil‍eUtils
import java.nio.file.Files
import java.nio.file.Paths
import scala.xml.Elem

class TestCreationForm extends FunSuite with CreationForm {
  val foaf = FOAFPrefix[Rdf]
        
  test("display form") {
    Fil‍eUtils.deleteLocalSPARL()
    val uri = // 
//      "http://usefulinc.com/ns/doap#Project"
     // "http://xmlns.com/foaf/0.1/Organization"
        foaf.Person.toString()
//    val store =  RDFStoreObject.store
    retrieveURI( ops.makeUri(uri), dataset )
    val rawForm = createElem(uri, lang="fr")
    val form = wrapWithHTML(rawForm)
    val file = "example.creation.form.html"
    Files.write(Paths.get(file), form.toString().getBytes );
    println( s"file created $file" )  
    
    assert ( rawForm . toString() . contains("homepage") )
  }

  def wrapWithHTML( e:Elem ) : Elem =
    <html>
    <head>
		  <style type="text/css">   
		  .resize {{ resize: both; width: 100%; height: 100%; }}
      .overflow {{ overflow: auto; width: 100%; height: 100%; }}
		  </style> 
		</head>
    <body>{e}</body>
    </html>

}