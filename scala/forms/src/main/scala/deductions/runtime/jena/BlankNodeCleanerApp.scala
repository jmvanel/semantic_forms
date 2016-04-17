package deductions.runtime.jena

import deductions.runtime.data_cleaning.BlankNodeCleanerBatch

object BlankNodeCleanerApp extends RDFStoreLocalJena1Provider
    with App
    with BlankNodeCleanerBatch[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  cleanUnreachableBlankNodeSubGraph()
}