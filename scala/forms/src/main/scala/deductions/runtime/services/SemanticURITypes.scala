package deductions.runtime.services

import org.w3.banana.RDFOpsModule
import deductions.runtime.dataset.RDFStoreLocalProvider
import org.w3.banana.RDF
import org.w3.banana.Prefix
import org.w3.banana.RDFOps
import org.w3.banana.RDF

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try
import scala.util.Success
import scala.util.Failure

import deductions.runtime.uri_classify.SemanticURIGuesser
import deductions.runtime.uri_classify.SemanticURIGuesser.SemanticURIType
//import deductions.runtime.jena.RDFStoreLocalJenaProvider
//import deductions.runtime.jena.RDFCache
import deductions.runtime.utils.MonadicHelpers

/** Banana principle: refer to concrete implementation only in blocks without code */
//object SemanticURITypes extends RDFCache
//    with SemanticURITypesTrait[Jena, Dataset]
//    with RDFStoreLocalJena1Provider {
//  val appDataStore = new RDFStoreLocalJena2Provider {}
//}

trait SemanticURITypesTrait[Rdf <: RDF, DATASET] extends RDFStoreLocalProvider[Rdf, DATASET] {

  import scala.concurrent.ExecutionContext.Implicits.global
  import ops._
  import rdfStore.transactorSyntax._

  val appDataStore: RDFStoreLocalProvider[Rdf, DATASET]
  val appDataPrefix = Prefix[Rdf]("appdata", "http://appdata/semanticURIType") // TODO better URI

  /**
   * get Semantic URI types From triple store or Internet,
   *  and store it;
   * from the list of ?O such that uri ?P ?O ,
   * return the list of their SemanticURIType as a Future;
   *
   * meant to be called in background
   */
  def getSemanticURItypesFromStoreOrInternet(uri: String)
  (implicit graph: Rdf#Graph)
  : Future[Iterator[(Rdf#Node, SemanticURIGuesser.SemanticURIType)]] = {
    val res = dataset.r({
      val triples: Iterator[Rdf#Triple] = find(graph,
        makeUri(uri), ANY, ANY)
      val semanticURItypes =
        for (triple <- triples) yield {
          val node = triple.objectt
          val (alreadyVisited, uriTypeFromTDB) = getSemanticURItypeFromAppDataStore(node)
          def uriTypeFromInternet() = if (isDereferenceableURI(node)) {
            SemanticURIGuesser.guessSemanticURIType(node.toString())
          } else
            // TODO get type from file ?
            Future.successful(SemanticURIGuesser.Unknown)

          val semanticURItype = if (alreadyVisited)
            Future successful uriTypeFromTDB
          else uriTypeFromInternet
          semanticURItype.map { uriType => (node, uriType) }
        }
      Future sequence semanticURItypes
    })
    res.get
  }

  def getSemanticURItypesFromStore(uri: String)
  (implicit graph: Rdf#Graph)
  : Seq[(Rdf#Node, SemanticURIGuesser.SemanticURIType)] = {
    val res = dataset.r({
      val triples: Iterator[Rdf#Triple] = find(graph,
        makeUri(uri), ANY, ANY)
      val semanticURItypes =
        for (triple <- triples) yield {
          val node = triple.objectt
          val (alreadyVisited, uriTypeFromTDB) = getSemanticURItypeFromAppDataStore(node)
          (node, uriTypeFromTDB)
        }
      semanticURItypes
    })
    res.map { iter => iter.toSeq }.getOrElse(Seq())
  }

  private def getSemanticURItypeFromAppDataStore(node: Rdf#Node)
  (implicit graph: Rdf#Graph)
  : (Boolean, SemanticURIType) = {
    val triples: Iterator[Rdf#Triple] = find(
      graph,
      node, appDataPrefix("semanticURIType"), ANY)
    if (!triples.isEmpty) (true,
      SemanticURIGuesser.makeSemanticURIType(triples.next().objectt.toString()))
    else (false, SemanticURIGuesser.Unknown)
  }

  private def isDereferenceableURI(node: Rdf#Node) = {
    if (isURI(node)) {
      val uri = node.toString()
      uri.startsWith("http:") ||
        uri.startsWith("https:") ||
        uri.startsWith("ftp:")
    } else false
  }

  private def isURI(node: Rdf#Node) = foldNode(node)(identity, x => None, x => None) != None
}
