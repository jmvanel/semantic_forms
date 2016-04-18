package deductions.runtime.sparql_cache.algos

import org.w3.banana.RDF
import org.w3.banana.io.RDFLoader
import scala.util.Try
import java.net.URL
import org.w3.banana.RDFOps

trait ChildrenFetcher[Rdf <: RDF]
    extends RDFLoader[Rdf, Try] {

  implicit val ops: RDFOps[Rdf]
  import ops._

  def fetch(url: URL, propertyFilter: List[Rdf#URI]): List[Rdf#Graph] = {
    val graph = load(url).getOrElse(emptyGraph)
    val v = getTriples(graph) collect {
      case triple if (propertyFilter.contains(triple.predicate) || propertyFilter.isEmpty) =>
        load(new URL(triple.objectt.toString())).getOrElse(emptyGraph)
    }
    v.toList
  }

  def fetch(url: URL, propertyFilter: List[Rdf#URI],
    propertyFilter2: List[Rdf#URI]): List[Rdf#Triple] = {
    val grs = fetch(url, propertyFilter)

    val trss = for {
      gr <- grs
      pr <- propertyFilter2
      trs = find(gr, ANY, pr, ANY)
    } yield trs
    trss.flatten
  }

  /** will be used for machine learning */
  def fetchDBPediaAbstractFromInterestsAndExpertise(url: URL): List[Rdf#Triple] = {
    ???
  }
}