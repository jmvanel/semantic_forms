package deductions.runtime.jena

import org.w3.banana.jena.Jena
import com.hp.hpl.jena.query.Dataset

import deductions.runtime.sparql_cache.RDFCacheAlgo

/**
 * @author jmv
 */
trait RDFCache extends RDFCacheAlgo[Jena, Dataset]