package deductions.runtime.abstract_syntax

import org.w3.banana.RDF

class ComputePropertiesList
[Rdf <: RDF, DATASET]
(subject: Rdf#Node,
    editable: Boolean = false, formuri: String)
    (implicit graph: Rdf#Graph) {
  self: FormSyntaxFactory[Rdf, DATASET] =>
    
    val classs = classFromSubject(subject) // TODO several classes
    val propsFromConfig = if(formuri=="")
      lookPropertieslistFormInConfiguration(classs)._1
    else {
      lookPropertiesListFromDatabaseOrDownload(formuri)._1
    }
    val propsFromSubject = fieldsFromSubject(subject, graph)
    val propsFromClass: Seq[Rdf#Node] =
      if (editable) {
        fieldsFromClass(classs, graph)
      } else Seq()
    val propertiesList0 = (propsFromConfig ++ propsFromSubject ++ propsFromClass).distinct
    val propertiesList = addRDFSLabelComment(propertiesList0)
  
}