package deductions.runtime.html

import java.net.URI

import deductions.runtime.core.FormModule
import deductions.runtime.utils.RDFPrefixesInterface

import scala.xml.NodeSeq

/**
 * HTML Form API, friendly for front end developer
 *
 *  Le but est de pouvoir facilement charger un des éléments de la génération de formulaire:
 *
 * - choix des champs à partir de l'instance
 * - choix des champs à partir de la classe
 * - génération de la syntaxe abstraite : getFieldStructure
 * - génération du HTML pour un champ : getFieldHTML
 * - génération du HTML pour tout le formulaire : getFormHTML
 *
 * suivant ce design pattern: template method:
 * https://en.wikipedia.org/wiki/Template_method_pattern
 * https://fr.wikipedia.org/wiki/Patron_de_m%C3%A9thode
 *
 */
trait HTMLFormAPI extends RDFPrefixesInterface {
  def getPropertiesFromInstance(instanceURI: URI): Seq[URI]
  def getPropertiesFromClass(classURI: URI): Seq[URI]
  def getFieldStructure(propertyURI: URI, instanceURI: URI): FormModule[String, String]#FormSyntax

  def getFieldHTML(propertyURI: URI, instanceURI: URI): NodeSeq
  def getFormHTML(properties: Seq[URI], instanceURI: URI): NodeSeq
}