package deductions.runtime.html

import java.net.URI
import deductions.runtime.abstract_syntax.FormModule
import scala.xml.NodeSeq

/** HTML Form API, friendly for front end developer */
trait HTMLFormAPI {
  def getPropertiesFromInstance( instanceURI: URI): Seq[URI]
  def getPropertiesFromClass( classURI: URI): Seq[URI]
  def getFieldStructure(propertyURI: URI, instanceURI: URI): FormModule[String, String]#FormSyntax
  
  def getFieldHTML(propertyURI: URI, instanceURI: URI): NodeSeq
  def getFormHTML(properties: Seq[URI], instanceURI: URI): NodeSeq
}