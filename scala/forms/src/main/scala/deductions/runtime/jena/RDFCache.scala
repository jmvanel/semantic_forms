package deductions.runtime.jena

import deductions.runtime.sparql_cache.RDFCacheAlgo

/**
 * TODO remove
 * @author jmv
 */
trait RDFCache
  extends RDFCacheAlgo[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
  with MicrodataLoaderModuleJena
