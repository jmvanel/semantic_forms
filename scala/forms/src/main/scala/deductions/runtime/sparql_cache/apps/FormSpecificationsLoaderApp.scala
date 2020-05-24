package deductions.runtime.sparql_cache.apps
import deductions.runtime.DependenciesForApps
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.sparql_cache.FormSpecificationsLoader
import deductions.runtime.utils.DefaultConfiguration

import scalaz._
import Scalaz._

/**
  * sbt runMain deductions.runtime.sparql_cache.apps.FormSpecificationsLoaderApp
  *
  * Created by LaFaucheuse on 06/07/2017.
  */
object FormSpecificationsLoaderApp extends {
  override val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
} with DependenciesForApps
  with FormSpecificationsLoader[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  resetCommonFormSpecifications()
  if (args.size === 0)
    loadCommonFormSpecifications()
  else
    loadFormSpecification(args(0))
  println(s"DONE load Common Form Specifications <${if(args.size > 0) args(0) else ""}>")
  // in named graph <$formSpecificationsGraphURI>")
  close(dataset)
}