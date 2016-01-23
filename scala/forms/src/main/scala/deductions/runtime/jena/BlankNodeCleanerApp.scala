package deductions.runtime.jena

import deductions.runtime.sparql_cache.BlankNodeCleaner

object BlankNodeCleanerApp extends RDFStoreLocalJena1Provider
    with App
    with BlankNodeCleaner[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  cleanUnreachableBlankNodeSubGraph()
}