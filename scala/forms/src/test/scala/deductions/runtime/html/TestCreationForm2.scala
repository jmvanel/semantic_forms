package deductions.runtime.html
import org.scalatest.FunSuite
import deductions.runtime.jena.RDFStoreObject
import org.w3.banana.PrefixBuilder
import org.w3.banana.GraphStore
import org.w3.banana.RDF
import org.w3.banana._
import org.w3.banana.syntax._
import org.w3.banana.diesel._
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.DirectoryNotEmptyException
import java.io.IOException
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.Path
import java.nio.file.FileSystems
import java.nio.file.FileVisitor
import java.nio.file.FileVisitResult
import java.nio.file.attribute.BasicFileAttributes

class TestCreationForm2 extends FunSuite with CreationForm {
  test("display form custom") {
    deleteLocalSPARL()
    val uri = "http://xmlns.com/foaf/0.1/Person"
    val store =  RDFStoreObject.store
    retrieveURI( Ops.makeUri(uri), store )
    store.appendToGraph( Ops.makeUri("test"), graphTest.personFormSpec )
    val form = create(uri, lang="fr") 
    println( form )
    assert ( ! form . toString() . contains("homepage") )
    assert (   form . toString() . contains("firstName") )
    assert (   form . toString() . contains("lastName") )
  }

  // TODO extract those 2 methods in separate class
  def deleteLocalSPARL() = {
    deleteRecursive("TDB")
  }
  def deleteRecursive(dir:String) = {
	  val path = FileSystems.getDefault.getPath(dir)
			  if (Files.exists(path) && Files.isDirectory(path)) {
				  Files.walkFileTree(path, new FileVisitor[Path] {
					  def visitFileFailed(file: Path, exc: IOException) = FileVisitResult.CONTINUE
						def visitFile(file: Path, attrs: BasicFileAttributes) = {
						  Files.delete(file)
						  FileVisitResult.CONTINUE
					  }
					  def preVisitDirectory(dir: Path, attrs: BasicFileAttributes) = FileVisitResult.CONTINUE
						def postVisitDirectory(dir: Path, exc: IOException) = {
						  Files.delete(dir)
						  FileVisitResult.CONTINUE
					  }
				  })
			  }
  }
  
  trait GraphTest[Rdf <: RDF] {
    implicit val ops: RDFOps[Rdf] = Ops.asInstanceOf[org.w3.banana.RDFOps[Rdf]]
    import ops._
    import syntax._
    val form = new PrefixBuilder[Rdf]("form", "http://deductions-software.com/ontologies/forms.owl.ttl#")
    val foaf = FOAFPrefix[Rdf]
    val personFormSpec = (
      URI("personForm")
      -- form("classDomain") ->- foaf.Person
      -- form("showProperties") ->- ( // list
        bnode("p1") -- rdf.first ->- foaf.firstName
                    -- rdf.rest ->- (
          bnode("p2") -- rdf.first ->- foaf.lastName
                      -- rdf.rest ->- rdf.nil))).graph
                      
//    val personFormSpec1 = (
//      URI("personForm")
//      -- form("classDomain") ->- foaf.Person
//      -- form("showProperties") ->- ( // list
//        bnode("p1") -- rdf.first ->- foaf.firstName
//                    -- rdf.rest ->- (
//          bnode("p2") -- rdf.first ->- foaf.lastName
//                      -- rdf.rest ->- rdf.nil))).graph
//                      
//    val personFormSpec2 = (
//      URI("personForm")
//      -- form("classDomain") ->- foaf.Person
//      -- form("showProperties") ->- ( // list
//        URI("p1") -- URI("rdffirst") ->- foaf.firstName
//                    -- URI("rdfrest") ->- (
//          URI("p2") -- URI("rdffirst1") ->- foaf.lastName
//                      -- URI("rdfrest1") ->- URI("rdf.nil") )))))).graph
  }
  
  val graphTest = new AnyRef with GraphTest[Rdf]
  println(TurtleWriter.asString(graphTest.personFormSpec, "" ))
}