package deductions.runtime.sparql_cache.algos

import org.w3.banana.RDF
import org.w3.banana.RDFOps
import deductions.runtime.abstract_syntax.InstanceLabelsInferenceMemory

/** UNUSED */
trait GraphEnrichment[Rdf <: RDF, DATASET]
  extends InstanceLabelsInferenceMemory[Rdf, DATASET] {

  implicit val ops: RDFOps[Rdf]
  import ops._

  private def enrichGraph(graph: Rdf#Graph, context: Map[String, String]) = {
    if (context.get("enrich").isDefined) {
      /* loop on each unique subject ?S in graph:
		   * - compute instance label ?LABEL
		   * - add triple ?S rdfs:label ?LABEL */
      val subjects = for (
        tr <- graph.triples;
        sub = tr.subject
      ) yield sub
      val addedTriples = for (subject <- subjects.toList.distinct) yield {
        val lang = context.get("lang").getOrElse("en")
        Triple(subject, rdfs.label,
          Literal.tagged(
            instanceLabelFromTDB( subject, lang = lang),
            Lang(lang)))
      }
      graph union (Graph(addedTriples))
    } else graph
  }
}