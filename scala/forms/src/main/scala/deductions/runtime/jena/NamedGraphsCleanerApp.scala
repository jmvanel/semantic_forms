package deductions.runtime.jena

import deductions.runtime.data_cleaning.NamedGraphsCleaner

object NamedGraphsCleanerApp extends RDFStoreLocalJena1Provider
    with App
    with NamedGraphsCleaner[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  cleanDBPediaGraphs()
}