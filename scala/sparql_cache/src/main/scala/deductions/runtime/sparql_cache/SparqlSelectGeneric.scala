package deductions.runtime.sparql_cache

import org.apache.jena.query._ ;
//import org.apache.jena.riot.RiotException ;
//import org.apache.jena.riot.RiotNotFoundException ;
//import org.apache.jena.riot.SysRIOT ;
//import org.apache.jena.shared.JenaException ;
//import org.apache.jena.shared.NotFoundException;
//import org.apache.jena.sparql.ARQInternalErrorException ;
import org.apache.jena.sparql.core.Transactional ;
import org.apache.jena.sparql.core.TransactionalNull;
//import org.apache.jena.sparql.mgt.Explain ;
import org.apache.jena.sparql.resultset.ResultsFormat ;
import org.apache.jena.system.Txn ;
import deductions.runtime.utils.RDFStoreLocalProvider
import org.w3.banana.RDF
import scala.util.Try
//import org.apache.jena.query.QueryCancelledException
//import org.apache.jena.query.QueryExecutionFactory
//import org.apache.jena.query.QueryFactory
import org.apache.jena.riot.resultset.rw.ResultsWriter
import org.apache.jena.riot.Lang
import java.io.ByteArrayOutputStream

trait SparqlSelectGeneric [Rdf <: RDF, DATASET] extends RDFStoreLocalProvider[Rdf, DATASET] {

  /**
   * sparql Select, CSV output;
   * it's actually pretty generic wrt output Syntax;
   *  inspired from class QueryExecUtils in Jena ARQ
   */
  def sparqlSelectWrite(
    queryString: String,
    ds0:         DATASET             = dataset,
    context:     Map[String, String] = Map(),
    outputSyntax: Lang = Lang.CSV): Try[String] = {

    import scala.language.implicitConversions
    implicit def funToRunnable(fun: () => Unit) = new Runnable() { def run() = fun() }

    val ds = ds0.asInstanceOf[org.apache.jena.query.Dataset]
    val transactional =
      if (ds != null && ds.supportsTransactions())
        ds
      else
        new TransactionalNull();
    var res = Try { "" }
    Txn.executeRead(transactional, () => {
      res = Try {
        val query = QueryFactory.create(queryString)
        val qe = QueryExecutionFactory.create(query, ds)
        val context = ARQ.getContext().copy();
        //            if ( prologue != null )
        //                context.set(ARQConstants.symPrologue, prologue);
        val results = qe.execSelect();
        val os = new ByteArrayOutputStream()
        ResultsWriter.create().context(context).lang(outputSyntax).build().write(
          os, results)
        os.toString()
      }
    });
    res
  }
}
