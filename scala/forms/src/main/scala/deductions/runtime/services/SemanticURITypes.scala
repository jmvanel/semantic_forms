package deductions.runtime.services

import org.w3.banana.jena.JenaModule
import org.w3.banana.RDFOpsModule
import deductions.runtime.dataset.RDFStoreLocalProvider
import org.w3.banana.RDF
import org.w3.banana.jena.Jena
import com.hp.hpl.jena.query.Dataset
import scala.concurrent.Future
import deductions.runtime.utils.MonadicHelpers
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import org.w3.banana.RDFOps
import org.w3.banana.RDF
import deductions.runtime.uri_classify.SemanticURIGuesser
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.jena.RDFStoreLocalJena2Provider
import deductions.runtime.uri_classify.SemanticURIGuesser.SemanticURIType
import org.w3.banana.Prefix
import deductions.runtime.jena.RDFStoreLocalJenaProvider

/** Banana principle: refer to concrete implementation only in blocks without code */
object SemanticURITypes extends RDFStoreLocalJena1Provider
    with SemanticURITypesTrait[Jena, Dataset] {
  val appDataStore = new RDFStoreLocalJena2Provider {}
}

trait SemanticURITypesTrait[Rdf <: RDF, DATASET] extends RDFStoreLocalProvider[Rdf, DATASET] {
  import scala.concurrent.ExecutionContext.Implicits.global
  import ops._
  val appDataStore: RDFStoreLocalProvider[Rdf, DATASET]
  val appDataPrefix = Prefix[Rdf]("appdata", "http://TODO/appdata") // TODO

  /**
   * get Semantic URI types From triple store or Internet,
   *  and store it;
   * from the list of ?O such that uri ?P ?O ,
   * return the list of their SemanticURIType as a Future;
   *
   * meant to be called in background
   */
  def getSemanticURItypesFromStoreOrInternet(uri: String): Future[Iterator[(Rdf#Node, SemanticURIGuesser.SemanticURIType)]] = {
    val res = rdfStore.r(dataset, {
      val triples: Iterator[Rdf#Triple] = ops.find(allNamedGraph,
        ops.makeUri(uri), ANY, ANY)
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

  def getSemanticURItypesFromStore(uri: String): Seq[(Rdf#Node, SemanticURIGuesser.SemanticURIType)] = {
    val res = rdfStore.r(dataset, {
      val triples: Iterator[Rdf#Triple] = ops.find(allNamedGraph,
        ops.makeUri(uri), ANY, ANY)
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

  private def getSemanticURItypeFromAppDataStore(node: Rdf#Node): (Boolean, SemanticURIType) = {
    val triples: Iterator[Rdf#Triple] = ops.find(
      // TODO : why asInstanceOf is needed ?
      appDataStore.allNamedGraph.asInstanceOf[Rdf#Graph],
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

  private def isURI(node: Rdf#Node) = ops.foldNode(node)(identity, x => None, x => None) != None
}