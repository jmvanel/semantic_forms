package deductions.runtime.sparql_cache.apps

import deductions.runtime.DependenciesForApps
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.sparql_cache.CommonVocabulariesLoader
import deductions.runtime.utils.DefaultConfiguration

/**
  * Created by LaFaucheuse on 06/07/2017.
  */
object CommonVocabulariesLoaderApp
  extends
  {
    val config = new DefaultConfiguration {
      override val useTextQuery = false
    }
  }
  with DependenciesForApps
  with CommonVocabulariesLoader[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
    loadCommonVocabularies()
  }
