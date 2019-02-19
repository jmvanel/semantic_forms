package deductions.runtime.connectors.icalendar

import org.w3.banana.RDF
import org.w3.banana.RDFOps
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.utils.RDFHelpers
import deductions.runtime.utils.RDFStoreLocalProvider

/** RDF graph to ICalendar converter
 *  see https://en.wikipedia.org/wiki/ICalendar
 *  Passed through validator https://icalendar.org/validator.html,
 *  Apple Calendar, Linux calcurse.
 *  */
trait RDF2ICalendar[Rdf <: RDF, DATASET] extends RDFPrefixes[Rdf] with RDFHelpers[Rdf] 
with RDFStoreLocalProvider[Rdf, DATASET]{
  implicit val ops: RDFOps[Rdf]
  import ops._

  /** main reusable function in API */
  def graph2iCalendar(graph: Rdf#Graph): String = {

    val eventTriples = find(graph, ANY, rdf.typ, schema("Event"))
    val events = new StringBuilder()
    val CRLF = "\r\n"

    def addLine(s: String): StringBuilder = {
      if (s != "") {
        events append s
        if ( ! s . endsWith(CRLF) )
          events append CRLF
      }
      return events
    }

    def processSubject( subject: Rdf#Node) = {
      addLine ("BEGIN:VEVENT")
      addLine( "UID:" + subject )
      for (
          triple2 <- find(graph, subject, ANY, ANY)
      ){ processTriple(triple2) }
      addLine( "END:VEVENT" )
    }

    def getDisplayLabel(objet: Rdf#Node, prop: Rdf#URI =URI("urn:displayLabel")) = {
//      println(s"getDisplayLabel: objet $objet ${getObjects(allNamedGraph, objet, URI("urn:displayLabel")).toList}")
      nodeToString(getObjects(allNamedGraph, objet, prop).toList.headOption.getOrElse(Literal("")))
    }

    def formatDateTime( objet: Rdf#Node): String = {
      val value = nodeToString(objet)
      if( value == "")
        return ""
      val t1 = value.replaceAll("-", "").replaceAll(":", "")
      val t2 = if ( t1 . contains("T"))
        t1
      else
        t1 + "T120000"
      "DTSTAMP:" + t2 + CRLF +
      "DTSTART:" + t2
    }

    def formatText(t: String): String =
      t.replaceAll("\n", CRLF+" ").grouped(60).fold("")((a,b) => a+b+CRLF+" ")

    def processTriple(triple: Rdf#Triple) = {
      val pred = triple.predicate;
      val objet = triple.objectt
      // println(s"processTriple( $triple")
      addLine( pred match {
        case dbo("startDate") => formatDateTime( objet )
        case schema("doorTime") => formatDateTime( objet )
        case URI("http://purl.org/NET/c4dm/event.owl#agent") =>
          "ORGANIZER:" + getDisplayLabel(objet)
        case dbo("endDate")   => "DTEND:" + nodeToString(objet).replaceAll("-", "")
        case URI("urn:displayLabel") => formatText("SUMMARY:" + nodeToString(objet))
        case rdfs.label              => formatText("SUMMARY:" + nodeToString(objet))
        case URI("http://purl.org/NET/c4dm/event.owl#place") =>
                                 "GEO:" + getDisplayLabel(objet)
        case rdfs.comment     => formatText("DESCRIPTION:" + nodeToString(objet))
        case dct("abstract")  => formatText("DESCRIPTION:" + nodeToString(objet))
        case con("participant")
                              => formatText("ATTENDEE;CN="+
                              getDisplayLabel(objet) +
                              ":MAILTO:" + getDisplayLabel(objet, foaf.mbox).replaceFirst("^mailto:", "") )
        case _                => ""
      })
    }

    for ( triple <- eventTriples) {
        processSubject(triple.subject);
    }

    return "BEGIN:VCALENDAR\r\n" +
      "VERSION:2.0\r\n" +
      "PRODID:https://github.com/jmvanel/semantic_forms/wiki\r\n" +
      events.toString() +
      "END:VCALENDAR\r\n"
  }
  
/* Output:
BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//hacksw/handcal//NONSGML v1.0//EN
BEGIN:VEVENT
UID:uid1@example.com
DTSTAMP:19970714T170000Z
ORGANIZER;CN=John Doe:MAILTO:john.doe@example.com
DTSTART:19970714T170000Z
DTEND:19970715T035959Z
SUMMARY:Bastille Day Party
GEO:48.85299;2.36885
END:VEVENT
END:VCALENDAR
Input:
<http://gmbvs.fr/events/row/26>
        a                   <http://dbpedia.org/ontology/Event> , <http://schema.org/Event> ;
        <http://www.w3.org/2000/01/rdf-schema#comment>
                "Vendredi 4 samedi 5 dimanche 6 octobre 2019"@fr ;
        <http://www.w3.org/2000/01/rdf-schema#label>
                "Week-end mycologique à Cerniébeau (39250)" ;
        <http://dbpedia.org/ontology/endDate>
                "2019-10-06"^^<http://www.w3.org/2001/XMLSchema#date> ;
        <http://dbpedia.org/ontology/startDate>
                "2019-10-04"^^<http://www.w3.org/2001/XMLSchema#date> ;
        <http://deductions.github.io/event.form.ttl#time>
                "" ;
        <http://purl.org/NET/c4dm/event.owl#agent>
                <http://gmvs69.free.fr/> ;
        <http://purl.org/NET/c4dm/event.owl#place>
                <http://dbpedia.org/resource/Cerniébeau> ;
        <http://purl.org/dc/terms/subject>
                <http://dbpedia.org/resource/Mycology>
 */
}