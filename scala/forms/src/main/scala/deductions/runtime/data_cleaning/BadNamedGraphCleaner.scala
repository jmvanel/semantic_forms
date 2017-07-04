package deductions.runtime.data_cleaning

import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.utils.{DefaultConfiguration, RDFHelpers, URIManagement}
import org.w3.banana.RDF

trait BadNamedGraphCleaner[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with RDFHelpers[Rdf]
    with URIManagement {

  /** */
  def cleanBadNamedGraphs() = {
    for (
      badGraphName <- listGraphNames() if (!isCorrectURI(badGraphName))
    //        graphToRecreate <- rdfStore.getGraph( dataset, URI(badGraphName) )
    ) {
      println(s"badGraphName <$badGraphName>")
    }
  }
}

object BadNamedGraphCleanerApp extends ImplementationSettings.RDFCache
    with App
    with BadNamedGraphCleaner[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  val config = new DefaultConfiguration {
    override val needLoginForEditing = true
    override val needLoginForDisplaying = true
    override val useTextQuery = false
  }
}