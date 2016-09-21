package deductions.runtime.abstract_syntax

import org.apache.log4j.Logger
import org.w3.banana.Prefix
import org.w3.banana.RDF
import deductions.runtime.services.Configuration
import deductions.runtime.utils.RDFHelpers
import deductions.runtime.sparql_cache.RDFCacheAlgo
import scala.util.Try

/**
 * Lookup Form specifications from RDF graph
 */
trait FormConfigurationFactory[Rdf <: RDF, DATASET]
    extends Configuration
    with RDFCacheAlgo[Rdf, DATASET]
//    with RDFStoreLocalProvider[Rdf, DATASET]
    with RDFHelpers[Rdf] {
  
  import ops._

  val formPrefix: Prefix[Rdf] = Prefix("form", formVocabPrefix)

  /**
   * lookup for form:showProperties (ordered list of fields) in Form Configuration within RDF graph about this class;
   *  usable for unfilled and filled Forms
   */
  def lookPropertiesListInConfiguration(classs: Rdf#URI)
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

 /** look for Properties List & form Configuration From URI in Database after trying to donwload
   *  TODO remove dependency to RDFCacheAlgo (dataset)
   *  PENDING : what to do when given formuri gives an empty propertiesList? */
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
    for (p <- props) { println( s"rdfProperty: $rdfProperty " + p) }
    val propertiesListFirst = props.headOption
    val propertiesList = nodeSeqToURISeq(rdfListToSeq(propertiesListFirst))
    propertiesList
  }

  /** lookup Form Spec from OWL class in Configuration */
  def lookFormSpecInConfiguration(classs: Rdf#URI)
  (implicit graph: Rdf#Graph)
  : Option[Rdf#Node] = {
    val forms = getSubjects(graph, formPrefix("classDomain"), classs) . toList
    Logger.getRootLogger().debug("forms " + forms.mkString( "; "))
    val formSpecOption = forms.flatMap {
      form => ops.foldNode(form)(uri => Some(uri), bn => Some(bn), lit => None)
    }.headOption
    if( forms.size > 1 )
    	Logger.getRootLogger().warn(
    	    s"WARNING: several form specs for $classs; chosen $formSpecOption")
//    Logger.getRootLogger().debug
    println( s"lookFormSpecInConfiguration: formNodeOption $formSpecOption" )
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
    prop: Rdf#Node)
    (implicit graph: Rdf#Graph): Seq[Rdf#Triple]
    = {
    find(graph, ANY, formPrefix("fieldAppliesToProperty"), prop).toSeq
  }

  def lookClassInFormSpec( formURI: Rdf#URI, formSpec: Rdf#Graph) = {
    val trOpt = find(formSpec, formURI, formPrefix("classDomain"), ANY).toSeq.headOption
    trOpt . map {
      tr => tr.objectt
    }.getOrElse(URI(""))
  }
    
}