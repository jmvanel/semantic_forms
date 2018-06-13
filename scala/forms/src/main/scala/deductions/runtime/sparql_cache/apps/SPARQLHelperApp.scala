package deductions.runtime.sparql_cache.apps

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.sparql_cache.SPARQLHelpers
import deductions.runtime.utils.DefaultConfiguration
import scala.util.Success
import scala.util.Failure
import scala.util.Try

/**
  * Created by LaFaucheuse on 06/07/2017.
  */
object SPARQLHelperApp extends ImplementationSettings.RDFModule
  with ImplementationSettings.RDFCache
  with SPARQLHelpers[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  val config = new DefaultConfiguration {
    override val useTextQuery = false
  }

  def selectJSON(queryString: String): String = {
    prinTry( sparqlSelectJSON(queryString) )
  }

  def selectXML(queryString: String): String = {
    prinTry( sparqlSelectXML(queryString) )
  }

  private def prinTry(ts: Try[String]) =
    ts match {
      case Success(s) => s
      case Failure(f) => f.getLocalizedMessage
   }
}
