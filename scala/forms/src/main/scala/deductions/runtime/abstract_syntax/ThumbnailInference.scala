package deductions.runtime.abstract_syntax

import org.w3.banana.RDF

import deductions.runtime.services.SPARQLHelpers

trait ThumbnailInference[Rdf <: RDF, DATASET]
    extends SPARQLHelpers[Rdf, DATASET]
    with FormModule[Rdf#Node, Rdf#URI] {

  import ops._

  val imagePproperties = Seq(
    foaf("thumbnail"),
    foaf("img"),
    dbo("thumbnail"),
    foaf("depiction"))

  def isImageTriple(subject: Rdf#Node, property: Rdf#Node, objet: Rdf#Node, objetType: Rdf#Node): Boolean =
    objetType == foaf("Image") ||
      imagePproperties.contains(property)

  def getURIimage(subject: Rdf#Node): Option[Rdf#Node] = {

    def getOne(property: Rdf#URI) = getObjects(allNamedGraph, subject, property).headOption
    val correspondingProperty = imagePproperties.find(property => {
      val opt = getOne(property); opt.isDefined
    })

    val imageURIOption = correspondingProperty.map {
      correspondingProperty => getObjects(allNamedGraph, subject, correspondingProperty).headOption
    }
    imageURIOption.flatten
  }
}