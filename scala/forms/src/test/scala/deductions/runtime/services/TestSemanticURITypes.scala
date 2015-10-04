package deductions.runtime.services

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.TimeoutException
import scala.concurrent.duration.Duration
import scala.concurrent.duration.MILLISECONDS
import scala.util.Failure
import scala.util.Success
import org.scalatest.Finders
import org.scalatest.FunSuite
import org.w3.banana.RDF
import org.w3.banana.jena.Jena
import com.hp.hpl.jena.query.Dataset
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.jena.RDFStoreLocalJena2Provider
import org.w3.banana.SparqlHttpModule
import org.w3.banana.RDFXMLReaderModule
import deductions.runtime.uri_classify.SemanticURIGuesser

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration._

trait TestSemanticURITypesTrait[Rdf <: RDF, DATASET] extends FunSuite
    with SemanticURITypesTrait[Rdf, DATASET]
    with RDFXMLReaderModule {

  test("check 1 URI") {
    //    val r = SemanticURIGuesser.guessSemanticURIType(
    //      //        "http://xmlns.com/foaf/0.1/Person")
    //      "http://xmlns.com/foaf/0.1/")
    //    r onComplete {
    //      case Success(t) => println(t)
    //      case Failure(x) => println(x)
    //    }
    //    val dur = 10000 // 1000000
    //    Await.ready(r, Duration(dur, MILLISECONDS))
    //    Thread.sleep(dur + 2000)
  }

  //  test("check some small FOAF profile") {
  //    import ops._
  //    val base = "http://www.agfa.com/w3c/jdroo/card.rdf"
  //    val profile = base + "#me"
  //    rdfStore.rw(dataset, {
  //      val is = new java.net.URL(base).openStream()
  //      val graph = rdfXMLReader.read(is, base = base).get
  //      rdfStore.appendToGraph(dataset, URI(profile), graph)
  //    })
  //    println(s"Loaded $profile in TDB")
  //    val r = getSemanticURItypesFromStoreOrInternet(profile)
  //    r onComplete {
  //        case Success(it) => println( it.mkString("\n") )
  //        case Failure(x) =>  println(x)
  //    }
  //    val dur = 10000
  //    Await.ready( r, Duration( dur, MILLISECONDS))
  //    Thread.sleep(dur+ 2000)
  //  }
}

class TestSemanticURITypes extends FunSuite
    with RDFStoreLocalJena1Provider
    with TestSemanticURITypesTrait[Jena, Dataset] {
  val appDataStore = new RDFStoreLocalJena1Provider {}
}
