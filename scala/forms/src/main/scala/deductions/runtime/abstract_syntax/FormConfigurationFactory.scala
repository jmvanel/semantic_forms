package deductions.runtime.abstract_syntax

import scala.util.Try

import org.w3.banana.Prefix
import org.w3.banana.RDF

import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.utils.RDFHelpers
import deductions.runtime.utils.RDFPrefixes

/**
 * Lookup Form specifications from RDF graph; TODO rename FormSpecificationFactory
 */
trait FormConfigurationFactory[Rdf <: RDF, DATASET]
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
  def lookPropertiesListInConfiguration(classs: Rdf#Node)
  (implicit graph: Rdf#Graph)
  : (Seq[Rdf#URI], Rdf#Node) = {
    val formSpecOption = lookFormSpecInConfiguration(classs)
    formSpecOption match {
      case None => (Seq(), URI(""))
      case Some(formConfiguration) =>
        val propertiesList = propertiesListFromFormConfiguration(formConfiguration)
        (propertiesList, formConfiguration)
    }
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
    val tryGraph = retrieveURINoTransaction( formConfiguration, dataset)
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
  def lookFormSpecInConfiguration(classs: Rdf#Node)
  (implicit graph: Rdf#Graph)
  : Option[Rdf#Node] = {
    val forms = getSubjects(graph, formPrefix("classDomain"), classs) . toList
    logger.debug("lookFormSpecInConfiguration: forms " + forms.mkString( "; "))
    val formSpecOption = forms.flatMap {
      form => ops.foldNode(form)(uri => Some(uri), bn => Some(bn), lit => None)
    }.headOption
    if( forms.size > 1 )
      logger.warn(s"WARNING: several form specs for $classs; chosen $formSpecOption")
    logger.info( s"lookFormSpecInConfiguration: found for <$classs> : $formSpecOption" )
    formSpecOption
  }

  /**
   * return e g :  <topic_interest>
   *  in :
   *  <pre>
   *  &lt;topic_interest&gt; :fieldAppliesToForm &lt;personForm> ;
   *   :fieldAppliesToProperty foaf:topic_interest ;
   *   :widgetClass form:DBPediaLookup .
   *  <pre>
   *  that is, query:
   *  ?S form:fieldAppliesToProperty prop .
   */
  def lookFieldSpecInConfiguration(
    prop: Rdf#Node)(implicit graph: Rdf#Graph): Seq[Rdf#Triple] =
    find(graph, ANY, formPrefix("fieldAppliesToProperty"), prop).toSeq

  def lookClassInFormSpec( formURI: Rdf#URI, formSpec: Rdf#Graph) = {
    val trOpt = find(formSpec, formURI, formPrefix("classDomain"), ANY).toSeq.headOption
    trOpt . map {
      tr => tr.objectt
    }.getOrElse(URI(""))
  }
    
}