package deductions.runtime.services

import org.w3.banana.jena.JenaModule
import org.w3.banana.RDFOpsModule
import deductions.runtime.jena.RDFStoreLocalJena1Provider
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

/** Banana principle: refer to concrete implementation only in blocks without code */
object SemanticURITypes extends RDFStoreLocalJena1Provider with SemanticURITypesTrait[Jena, Dataset]

trait SemanticURITypesTrait[Rdf <: RDF, DATASET] extends RDFStoreLocalProvider[Rdf, DATASET] {

  import ops._

  /**
   * From the list of ?O such that uri ?P ?O ,
   *  return the list of their SemanticURIType as a Future
   */
  def getSemanticURItypes(uri: String): Future[Iterator[(Rdf#Node, SemanticURIGuesser.SemanticURIType)]] = {
    Future.successful(Seq[(Rdf#Node, SemanticURIGuesser.SemanticURIType)]().toIterator)
        import scala.concurrent.ExecutionContext.Implicits.global
    
        val r = rdfStore.r(dataset, {
          for (
            // TODO use allNamedGraphs from RDFStoreObject
            allNamedGraphs <- rdfStore.getGraph(dataset, ops.makeUri("urn:x-arq:UnionGraph"))
          ) yield {
            // get the list of ?O such that uri ?P ?O .
            val triples: Iterator[Rdf#Triple] = ops.find(allNamedGraphs,
              ops.makeUri(uri), ANY, ANY)
            val semanticURItypes =
              for (triple <- triples) yield {
                val node = triple.objectt // getObject
                val semanticURItype = if (isDereferenceableURI(node)) {
                  SemanticURIGuesser.guessSemanticURIType(node.toString())
                } else
                  Future.successful(SemanticURIGuesser.Unknown)
                semanticURItype.map { st => (node, st) }
              }
            Future sequence semanticURItypes
          }
        })
        val r1 = r.flatMap(identity)
        val rr = MonadicHelpers.tryToFuture(r1)
        rr.flatMap(identity)
  }

  def isDereferenceableURI(node: Rdf#Node) = {
    if (isURI(node)) {
      val uri = node.toString()
      uri.startsWith("http:") ||
        uri.startsWith("https:") ||
        uri.startsWith("ftp:")
    } else false
  }

  private def isURI(node: Rdf#Node) = ops.foldNode(node)(identity, x => None, x => None) != None
}