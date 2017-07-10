package deductions.runtime.sparql_cache.apps

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.sparql_cache.SPARQLHelpers
import deductions.runtime.utils.DefaultConfiguration
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
    sparqlSelectJSON(queryString)
  }

  def selectXML(queryString: String): String = {
    sparqlSelectXML(queryString)
  }
  //  def select(queryString: String): String = {
  //    sparqlSelectQuery(queryString).toString()
  //  }

}
