package controllers

import play.api.mvc.Accepting
import play.api.mvc.AcceptExtractors

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

}