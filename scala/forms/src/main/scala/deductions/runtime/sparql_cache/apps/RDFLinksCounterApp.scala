package deductions.runtime.sparql_cache.apps

import deductions.runtime.utils.DefaultConfiguration
import deductions.runtime.DependenciesForApps
import deductions.runtime.rdf_links_rank.RDFLinksCounter
import deductions.runtime.jena.ImplementationSettings

/** one time app populating RDF Links Counts */
object RDFLinksCounterApp
    extends { override val config = new DefaultConfiguration {} } with DependenciesForApps
    with RDFLinksCounter[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  resetRDFLinksCounts(
    dataset,
    linksCountDataset = dataset,
    linksCountGraphURI = defaultLinksCountGraphURI)

  computeLinksCount(
    dataset,
    linksCountDataset = dataset,
    linksCountGraphURI = defaultLinksCountGraphURI)
}

object RDFLinksCounterResetApp
    extends { override val config = new DefaultConfiguration {} } with DependenciesForApps
    with RDFLinksCounter[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  resetRDFLinksCounts(
    dataset,
    linksCountDataset = dataset,
    linksCountGraphURI = defaultLinksCountGraphURI)
}
