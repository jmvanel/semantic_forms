package deductions.runtime.abstract_syntax

import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.utils.{RDFHelpers, RDFPrefixes}
import deductions.runtime.core.HTTPrequest

import org.w3.banana.{Prefix, RDF}

import scala.util.Try

/**
 * Lookup Form specifications from RDF graph
 */
trait FormSpecificationFactory[Rdf <: RDF, DATASET]
    extends
    RDFCacheAlgo[Rdf, DATASET]
    with RDFHelpers[Rdf]
    with RDFPrefixes[Rdf] {
  
  import ops._
  val formPrefix: Prefix[Rdf] = form

  /**
   * lookup for form:showProperties (ordered list of fields) in Form Configuration within RDF graph about this class;
   *  usable for unfilled and filled Forms
   *  @return Seq of pairs (properties RDF List, form Configuration node)
   */
  def lookPropertiesListInConfiguration(
      // TODO classs: Seq[Rdf#Node])
      classs: Rdf#Node)
  (implicit graph: Rdf#Graph)
  : (Seq[Rdf#URI], Rdf#Node) = {
    val formSpecs = lookFormSpecsInConfiguration(
        Seq(classs)) // TODO
      
    val v = for( formSpec <- formSpecs ) yield {
        val propertiesList = propertiesListFromFormConfiguration(formSpec)
        (propertiesList, formSpec)
    }

    // TODO
    v.headOption.getOrElse((Seq(), URI("")))
  }

 /** look for Properties List & form Configuration From URI in Database,
  *   after trying to download form Configuration from its URI;
   *  TODO remove dependency to RDFCacheAlgo (dataset)
   *  PENDING : what to do when given formuri gives an empty propertiesList?
   *  
   *  @return properties List, form Configuration URI,
   *  Try[Graph]: possibly downloaded form Configuration from its URI */
  def lookPropertiesListFromDatabaseOrDownload(formuri: String)
      (implicit graph: Rdf#Graph):
       (Seq[Rdf#URI], Rdf#Node, Try[Rdf#Graph]) = {
    val formConfiguration = URI(formuri)
    val tryGraph = retrieveURIBody( formConfiguration, dataset, HTTPrequest(), transactionsInside=false )
    val propertiesList = propertiesListFromFormConfiguration(formConfiguration)
    (propertiesList, formConfiguration, tryGraph)
  }
  
  /** from given form Configuration, @return ordered RDF properties List 
   *  TODO consider returning Seq[Rdf#Node] */
  def propertiesListFromFormConfiguration(formConfiguration: Rdf#Node)
  (implicit graph: Rdf#Graph)
  : Seq[Rdf#URI] = listFromFormConfiguration(formConfiguration, formPrefix("showProperties") )

  protected def listFromFormConfiguration(formConfiguration: Rdf#Node, rdfProperty: Rdf#URI)
  (implicit graph: Rdf#Graph)
  : Seq[Rdf#URI] = {
    val props = getObjects(graph, formConfiguration, rdfProperty)
    for (p <- props) { logger.debug( s"listFromFormConfiguration: rdfProperty: <$rdfProperty> <$p>") }
    val propertiesListFirst = props.headOption
    val propertiesList = nodeSeqToURISeq(rdfListToSeq(propertiesListFirst))
    propertiesList
  }

  /** lookup Form Spec from OWL class in Configuration */
  private def lookFormSpecsInConfiguration(classes: Seq[Rdf#Node])
  (implicit graph: Rdf#Graph): Seq[Rdf#Node] = {
    val v = for (classe <- classes if classe != nullURI ) yield {
      val forms = getSubjects(graph, formPrefix("classDomain"), classe).toList
      logger.debug("lookFormSpecInConfiguration: forms " + forms.mkString("; "))
      val formSpecOption = forms.flatMap {
        form => ops.foldNode(form)(uri => Some(uri), bn => Some(bn), lit => None)
      }.headOption
      if (forms.size > 1)
        logger.warn(
          s"WARNING: several form specs for <$classe>; chosen $formSpecOption \n\tother form specs: ${
            forms.filter(_ != formSpecOption.getOrElse(nullURI)).mkString("", "\n\t", "")
          }")
      logger.debug(s"lookFormSpecInConfiguration: found for <$classe> : formSpecOption $formSpecOption")
      formSpecOption
    }
    v.flatten
  }

  /**
   * return e g :  <topic_interest>
   *  in :
   *  <pre>
   *  &lt;topic_interest&gt; :fieldAppliesToForm &lt;personForm> ;
   *   :fieldAppliesToProperty foaf:topic_interest ;
   *   :widgetClass form:DBPediaLookup .
   *  <pre>
   *
   *  that is, query:
   *  ?S form:fieldAppliesToProperty <prop> .
   */
  def lookFieldSpecInConfiguration(
    prop: Rdf#Node)(implicit graph: Rdf#Graph): Seq[Rdf#Triple] =
    find(graph, ANY, formPrefix("fieldAppliesToProperty"), prop).toSeq

  /** look for associated Class In given Form Spec (via predicate form:classDomain) */
  def lookClassInFormSpec( formURI: Rdf#URI, formSpec: Rdf#Graph): Rdf#Node = {
    val trOpt = find(formSpec, formURI, formPrefix("classDomain"), ANY).toSeq.headOption
    trOpt . map {
      tr => tr.objectt
    }.getOrElse(URI(""))
  }
    
}