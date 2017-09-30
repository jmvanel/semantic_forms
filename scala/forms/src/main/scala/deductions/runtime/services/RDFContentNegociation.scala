package deductions.runtime.services

//import org.w3.banana.io.{
//  N3,
//  Turtle,
//  NTriples,
//  RDFXML,
//  RDFaXHTML,
//  SparqlAnswerJson,
//  SparqlAnswerXml,
//  JsonLd,
//  JsonLdCompacted,
//  JsonLdExpanded,
//  JsonLdFlattened
//}

/** RDF Content Negociation, alias conneg */
trait RDFContentNegociation {

  val rdfXMLmime = "application/rdf+xml"
  val turtleMime = "text/turtle"
  val jsonldMime = "application/ld+json"

  val htmlMime = "text/html"


  /** order of arguments is historical order of RDF syntaxes */
  def foldRdfSyntax[I, O](mimeType: String, input: I = Unit)(
    funRdfXML: I => O,
    funTurtle: I => O,
    funJsonld: I => O): (O, Boolean) = {

    val fun = Map(
        rdfXMLmime -> funRdfXML,
        turtleMime -> funTurtle,
        jsonldMime -> funJsonld)

    val knownMIME = fun.get(mimeType).isDefined
    (fun.getOrElse(mimeType, funTurtle)(input), knownMIME)
  }

  def isKnownRdfSyntax(mimeType: String) = {
    val result = foldRdfSyntax(mimeType)(
      identity,
      identity,
      identity)
    result._2
  }
}