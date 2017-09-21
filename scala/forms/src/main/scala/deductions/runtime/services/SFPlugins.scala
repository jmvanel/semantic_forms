package deductions.runtime.services

import org.w3.banana.RDF

/**
 * configure SF Plugins;
 * see also
 * forms_play/app/controllers/SemanticController.scala
 */
trait SFPlugins[Rdf <: RDF, DATASET] {
  self: ApplicationFacadeImpl[Rdf, DATASET] =>
  addSaveListener(this) // for TimeSeries
  addSaveListener(new RDFLinksCounterListenerClass(config))
  addServiceListener(new RDFLinksCounterLoadListenerClass(config, this))
}