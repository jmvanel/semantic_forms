package deductions.runtime.jena

import deductions.runtime.sparql_cache.BlankNodeCleanerBatch

object BlankNodeCleanerApp extends RDFStoreLocalJena1Provider
    with App
    with BlankNodeCleanerBatch[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  cleanUnreachableBlankNodeSubGraph()
}