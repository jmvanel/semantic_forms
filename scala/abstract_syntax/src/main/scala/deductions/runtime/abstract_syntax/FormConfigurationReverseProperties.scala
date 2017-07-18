package deductions.runtime.abstract_syntax

import deductions.runtime.core.FormModule
import org.w3.banana.RDF

trait FormConfigurationReverseProperties[Rdf <: RDF, DATASET]
    extends FormSpecificationFactory[Rdf, DATASET]
    with FormModule[Rdf#Node, Rdf#URI] {

  /**
   * from given form Configuration, @return ordered RDF properties List
   *  TODO consider returning Seq[Rdf#Node]
   */
  def reversePropertiesListFromFormConfiguration(formConfiguration: Rdf#Node)(implicit graph: Rdf#Graph): Seq[Rdf#URI] =
    listFromFormConfiguration(formConfiguration, formPrefix("showReverseProperties"))

  def addInverseTriples(fields2: Seq[Entry],
                        step1: FormSyntax): Seq[Entry] = {
    for ( reverseProperty <- step1.reversePropertiesList ) yield {
      /* ResourceEntry(label: String, comment: String,
    property: ObjectProperty = nullURI, validator: ResourceValidator,
    value: URI = nullURI, val alreadyInDatabase: Boolean = true,
    possibleValues: Seq[(NODE, NODE)] = Seq(),
    val valueLabel: String = "",
    type_ : NODE = nullURI,
    inverseTriple = true)
       */
      reverseProperty
    }
     fields2 // TODO <<<<<<<<<<<<<<<<<<
  }
}