package deductions.runtime.services

import org.apache.jena.sparql.function.library.substring
import deductions.runtime.utils.StringHelpers

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

/** RDF Content Negociation, alias conneg;
 *  NOTE: there is in Play! Request
 *  `def accepts(mimeType: String): Boolean`
 *  but we want to limit dependency on Play */
trait RDFContentNegociation extends StringHelpers {

  val rdfXMLmime = "application/rdf+xml"
  val turtleMime = "text/turtle"
  val n3Mime = "text/n3"
  val jsonldMime = "application/ld+json"

  val htmlMime = "text/html"

  val extensionToMime = Map(
      "owl" -> rdfXMLmime,
      "rdf" -> rdfXMLmime,
      "ttl" -> turtleMime,
      "jsonld" -> jsonldMime,
      "n3" ->  n3Mime
      )
  /** order of arguments is historical order of RDF syntaxes */
  def foldRdfSyntax[I, O](mimeType: String, input: I = Unit)(
    funRdfXML: I => O,
    funTurtle: I => O,
    funJsonld: I => O,
    funN3: I => O
    ): (O, Boolean) = {

    val fun = Map(
        rdfXMLmime -> funRdfXML,
        turtleMime -> funTurtle,
        jsonldMime -> funJsonld,
        n3Mime -> funN3)

    val knownMIME = fun.get(mimeType).isDefined
    (fun.getOrElse(mimeType, funTurtle)(input), knownMIME)
  }

  def isKnownRdfSyntax(mimeType: String): Boolean = {
    val result = foldRdfSyntax(mimeType)(
      identity,
      identity,
      identity,
      identity)
    result._2
  }

  def stringMatchesRDFsyntax(s: String): Option[String] = {
    val sLowerCase = s.toLowerCase()
    def testMime(pattern: String) = sLowerCase.matches(s".*$pattern.*")
    sLowerCase match {
      case _ if (testMime("rdf"))    => Some(rdfXMLmime)
      case _ if (testMime("turtle")) => Some(turtleMime)
      case _ if (testMime("json"))   => Some(jsonldMime)
      case _                         => None
    }
  }

  def computeMIME(accepts: String, default: String=jsonldMime): String = {
     if( isKnownRdfSyntax(accepts) )
         accepts
     else
       default
  }

  def computeMIMEOption(accepts: Option[String], default: String = jsonldMime): String = {
    accepts match {
      case Some(a) => computeMIME(a)
      case None => default
    }
  }

  def getMimeFromURI(uri: String): Option[String] = {
    val r = for (
      extension <- substringAfterLastIndexOf(uri, ".")
//      ; _ = println( s"getMimeFromURI($uri) -> $extension")
    ) yield extensionToMime.get(extension)
    r.flatten
  }

}