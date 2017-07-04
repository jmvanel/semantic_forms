package deductions.runtime

import deductions.runtime.jena.{ImplementationSettings, RDFCache, RDFStoreLocalJena1Provider}
import deductions.runtime.sparql_cache.SitesURLForDownload

/**
 * Dependencies For Apps, be it simple command line or Web app;
 * provides RDFStore in Local Jena TDB & Default Configuration
 * CAUTION:
 * Concrete objects must
 * `
 * extends { override val config = new DefaultConfiguration{} }
 * `
 */
trait DependenciesForApps
  extends DependenciesForAppsNoCache
  with RDFCache
  with SitesURLForDownload

/**
 * NOTES:
 * - mandatory that JenaModule (RDFModule) is first; otherwise ops may be null
 * - mandatory that DependenciesForApps* is first;
 *   otherwise allNamedGraph may be null
 */
trait DependenciesForAppsNoCache
  extends ImplementationSettings.RDFModule
  with RDFStoreLocalJena1Provider
  with App