package deductions.runtime.dataset

import org.w3.banana.{GraphStore, RDF, RDFOpsModule}

import scala.util.{Success, Try}

/** */
trait DatasetUtils[Rdf <: RDF]
    extends DatasetFactory
    with RDFOpsModule {

  /** merge given datasets */
  def merge(datasets: Seq[Dataset]): GraphStore[Rdf, Try, Dataset] = {
    val output: Dataset = createInMemory
    val gs: GraphStore[Rdf, Try, Dataset] =
      new GraphStore[Rdf, Try, Dataset] {
        def appendToGraph(a: Dataset, uri: Rdf#URI, graph: Rdf#Graph): Try[Unit] = ??? //         {// TODO        }
        def getGraph(a: Dataset, uri: Rdf#URI): Try[Rdf#Graph] = {
          val i = datasets.toIterator
          def f: Try[Rdf#Graph] = {
            val r = while (i.hasNext) {
              val res = getGraph(i.next(), uri)
              if (res.isSuccess || !i.hasNext) {
                return res
              }
            }
            Success(ops.emptyGraph)
          }
          f
        }
        def removeGraph(a: Dataset, uri: Rdf#URI): scala.util.Try[Unit] = ??? // {// TODO        }
        def removeTriples(a: Dataset, uri: Rdf#URI, triples: Iterable[org.w3.banana.TripleMatch[Rdf]]): scala.util.Try[Unit] = ??? // {// TODO     }
      }
    gs
  }

}
