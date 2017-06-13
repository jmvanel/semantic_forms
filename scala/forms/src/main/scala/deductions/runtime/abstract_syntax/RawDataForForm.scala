package deductions.runtime.abstract_syntax

import scala.xml.Node

/** intermediary data for form generation:  properties' List, etc */
case class RawDataForForm[Node, URI <: Node](
                                              // TODO make it functional #170 : rename fields like FormSyntax
                                              entriesList: Seq[FormModule[Node, URI]#Entry],
                                              classs: Node, // = nullURI,
                                              subject: Node, // = nullURI,
                                              editable: Boolean = false,
                                              formURI: Option[Node] = None,
                                              // PENDING rather a Boolean field of Entry
                                              reversePropertiesList: Seq[Node] = Seq(),
                                              /* properties Groups come from multiple super-classes */
                                              propertiesGroups: collection.Map[Node, RawDataForForm[Node, URI]] =
                                              collection.Map[Node, RawDataForForm[Node, URI]]())
{

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

  /*def makeEntries(propertiesList: Seq[Node]): Seq[Entry] =
    propertiesList.map {
      prop => makeEntry(prop)
    }*/

  /*def makeRawDataForForm(propertiesList: Seq[Node]): RawDataForForm[Node, URI] = {
    val entries = makeEntries(propertiesList)
    RawDataForForm[Node, URI]( entries, nullURI, nullURI)
  }*/
  def makeRawDataForForm(propertiesList: Seq[Node]): FormSyntax = {
    val entries = makeEntries(propertiesList)
    FormSyntax(nullURI, Seq(), entries)
  }
}
