package deductions.runtime.services

import org.w3.banana.RDFOpsModule
import deductions.runtime.jena.RDFStoreObject
import deductions.runtime.sparql_cache.RDFCache
import org.w3.banana.SparqlGraphModule
import org.w3.banana.SparqlOpsModule
import org.w3.banana.PointedGraph
import org.w3.banana.diesel._
import org.scalatest.FunSuite
import org.w3.banana.jena.Jena
import com.hp.hpl.jena.query.Dataset
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import org.w3.banana.RDF

trait TestStringSearchTrait[Rdf <: RDF, DATASET] extends FunSuite
      with RDFOpsModule
      with StringSearchSPARQL[Rdf, DATASET] {

    lazy val search // : StringSearchSPARQL2[Rdf, DATASET]
    		= this;
    val res = search.search("Jean")
    import scala.concurrent.ExecutionContext.Implicits.global
    res.onSuccess {
        case r => println(r) }
    Thread.sleep(2000) // TODO tests
}

class TestStringSearch extends  FunSuite
with RDFStoreLocalJena1Provider // CAUTION : should be first !
with TestStringSearchTrait[Jena, Dataset] 
 