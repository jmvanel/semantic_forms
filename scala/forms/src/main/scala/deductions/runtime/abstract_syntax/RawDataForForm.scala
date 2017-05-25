package deductions.runtime.abstract_syntax

/** intermediary data for form generation:  properties' List, etc */
case class RawDataForForm[Node, URI](
    entriesList: Seq[FormModule[Node, URI]#Entry], // Seq[Node],
    classs: Node, // = nullURI,
    subject: Node, // = nullURI,
    editable: Boolean = false,
    formURI: Option[Node] = None,
    reversePropertiesList: Seq[Node] = Seq(),
    /* properties Groups come from multiple super-classes */
    propertiesGroups: collection.Map[Node, RawDataForForm[Node, URI]] = collection.Map[Node, RawDataForForm[Node, URI]]())
    extends FormModule[Node, Node]
    {

//  type Entry = FormModule[Node, Node]#Entry

  def setSubject(subject: Node, editable: Boolean): RawDataForForm[Node, URI] = {
    val propertiesGroupsWithSubject = propertiesGroups.map {
      case (node, rawDataForForm) => (node,
        rawDataForForm.setSubject(subject, editable))
    }
    RawDataForForm[Node, URI](entriesList, classs, subject, editable, formURI, reversePropertiesList,
      propertiesGroupsWithSubject)
  }
  
  def propertiesList: Seq[Node] = {
    entriesList . map ( _.property )
  }
}

trait RawDataForFormModule[Node, URI <: Node] extends FormModule[Node, URI] {
  
  def makeEntries(propertiesList: Seq[Node]): Seq[Entry] =
    propertiesList . map {
      prop =>
        new Entry { override val property = prop }}

  def makeRawDataForForm(propertiesList: Seq[Node]): RawDataForForm[Node, URI] = {
    val entries = makeEntries(propertiesList)
//    propertiesList . map {
//      prop =>
//        new Entry { override val property = prop }}
    RawDataForForm[Node, URI]( entries, nullURI, nullURI)
  }
}
