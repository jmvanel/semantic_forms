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
import org.apache.jena.query.ResultSet
import org.apache.jena.query.Syntax

/** stuff that is not in Banana :( ; not standard anyway */
class JenaComplements(implicit ops: RDFOps[Jena]) extends SparqlComplements[Jena, Dataset] {

  private lazy val querySolution = new QuerySolution(ops)

  /** Executes a Construct query with Union Graph. */
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

  /** Executes a Construct query with ARQ Syntax (Jena);
   *  see https://issues.apache.org/jira/browse/JENA-1629,
   *  https://issues.apache.org/jira/browse/JENA-1629 */
  def executeConstructArqSyntax(dataset: Dataset, query: String,
                                bindings: Map[String, Jena#Node]): Try[Dataset] = Try {
    val qexec: QueryExecution =
      if (bindings.isEmpty)
        QueryExecutionFactory.create(query, Syntax.syntaxARQ, dataset)
      else
      	QueryExecutionFactory.create(query, Syntax.syntaxARQ, dataset, querySolution.getMap(bindings))
      qexec.execConstructDataset()
  }

  /** Executes a SELECT query with Union Graph. */
  def executeSelectUnionGraph(dataset: Dataset, query: Jena#ConstructQuery,
		                          bindings: Map[String, Jena#Node]) : Try[ResultSet]= Try {
    val qexec: QueryExecution =
      if (bindings.isEmpty)
        QueryExecutionFactory.create(query, dataset)
      else
        QueryExecutionFactory.create(query, dataset, querySolution.getMap(bindings))

    qexec.getContext().set(TDB.symUnionDefaultGraph, true)
    qexec.execSelect()
  }
}