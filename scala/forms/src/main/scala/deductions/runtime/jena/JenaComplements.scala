package deductions.runtime.jena

import scala.util.Try
import org.w3.banana.RDFOps
import org.w3.banana.RDF
import org.apache.jena.query.QueryExecution
import org.apache.jena.query.QueryExecutionFactory
import org.w3.banana.RDFStore
import org.w3.banana.jena.util.QuerySolution
import org.w3.banana.jena.Jena
import org.apache.jena.query.Dataset
import org.apache.jena.tdb.TDB
import deductions.runtime.utils.SparqlComplements

class JenaComplements(implicit ops: RDFOps[Jena]) extends SparqlComplements[Jena, Dataset] {

  lazy val querySolution = new QuerySolution(ops)

  /** Executes a Construct query. */
  def executeConstructUnionGraph(dataset: Dataset, query: Jena#ConstructQuery,
                                 bindings: Map[String, Jena#Node]): Try[Jena#Graph] = Try {
    val qexec: QueryExecution =
      if (bindings.isEmpty)
        QueryExecutionFactory.create(query, dataset)
      else
        QueryExecutionFactory.create(query, dataset, querySolution.getMap(bindings))

    qexec.getContext().set(TDB.symUnionDefaultGraph, true)
    val result = qexec.execConstruct()
    result.getGraph()
  }

}