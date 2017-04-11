package deductions.runtime.data_cleaning

import scala.util.Success

import org.w3.banana.RDF

import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.dataset.RDFStoreLocalUserManagement
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.Authentication
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.services.SPARQLHelpers
import deductions.runtime.utils.URIManagement
import deductions.runtime.utils.RDFHelpers

trait BadNamedGraphCleaner[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with RDFHelpers[Rdf]
    with URIManagement {

  import ops._

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