package deductions.runtime.abstract_syntax

import org.w3.banana.RDF
import deductions.runtime.services.Configuration
import deductions.runtime.utils.RDFHelpers

trait FormConfigurationReverseProperties[Rdf <: RDF, DATASET]
    extends Configuration
    with FormConfigurationFactory[Rdf, DATASET]
{
  
  import ops._
  
    /** from given form Configuration, @return ordered RDF properties List 
   *  TODO consider returning Seq[Rdf#Node] */
  def reversePropertiesListFromFormConfiguration(formConfiguration: Rdf#Node)
  (implicit graph: Rdf#Graph): Seq[Rdf#URI] =
    listFromFormConfiguration(formConfiguration, formPrefix("showReverseProperties") )
}