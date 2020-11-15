package controllers

import play.api.mvc.Accepting
import play.api.mvc.AcceptExtractors
import play.api.http.MediaRange

trait RDFContentNegociationPlay
extends AcceptExtractors {

  // TODO reuse trait RDFContentNegociation
  protected val AcceptsTTL = Accepting("text/turtle")
  protected val AcceptsJSONLD = Accepting("application/ld+json")
  protected val AcceptsRDFXML = Accepting("application/rdf+xml")
  protected val AcceptsSPARQLresults = Accepting("application/sparql-results+json")
  protected val AcceptsSPARQLresultsXML = Accepting("application/sparql-results+xml")
  protected val AcceptsICal = Accepting("text/calendar")

  protected val turtle = AcceptsTTL.mimeType

	/** mime Abbreviations, format = "turtle" or "rdfxml" or "jsonld" */
	val mimeAbbrevs = Map(
	    AcceptsTTL -> "turtle",
	    AcceptsJSONLD -> "jsonld",
	    AcceptsRDFXML -> "rdfxml",
	    Accepts.Json -> "json",
	    Accepts.Xml -> "xml",
	    AcceptsSPARQLresults -> "json",
	    AcceptsSPARQLresultsXML -> "xml",
	    AcceptsICal -> "ical"
	 )

	 val simpleString2mimeMap = mimeAbbrevs.map(_.swap)

  val mimeSet = mimeAbbrevs.keys.toSet
  protected def computeMIME(accepts: Accepting, default: Accepting): Accepting = {
    if( mimeSet.contains(accepts))
       accepts
    else default
  }

  protected def computeMIME(accepts: Seq[MediaRange], default: Accepting): Accepting = {
    val v = accepts.find {
      mediaRange => val acc = Accepting(mediaRange.toString())
      mimeSet.contains(acc) }
    v match {
      case Some(acc) => Accepting(acc.toString())
      case None => default
    }
  }
}
