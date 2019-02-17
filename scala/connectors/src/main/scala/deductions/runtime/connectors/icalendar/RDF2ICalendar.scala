package deductions.runtime.connectors.icalendar

import org.w3.banana.RDF
import org.w3.banana.RDFOps
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.utils.RDFHelpers

/** RDF graph to ICalendar converter
 *  see https://en.wikipedia.org/wiki/ICalendar
 *  Passed through validator https://icalendar.org/validator.html
 *  */
trait RDF2ICalendar[Rdf <: RDF] extends RDFPrefixes[Rdf] with RDFHelpers[Rdf] {
  implicit val ops: RDFOps[Rdf]
  import ops._

  /** main reusable function in API */
  def graph2iCalendar(graph: Rdf#Graph): String = {

    val eventTriples = find(graph, ANY, rdf.typ, schema("Event"))
    val events = new StringBuilder()

    def addLine(s: String): StringBuilder = {
      if (s != "") {
        events append s
        events append "\r\n"
      }
      return events
    }

    def processSubject( subject: Rdf#Node) = {
      addLine ("BEGIN:VEVENT")
      addLine( "UID:" + subject )
      for (
          triple2 <- find(graph, subject, ANY, ANY);
          _ = processTriple(triple2)
      ){}
      addLine( "END:VEVENT" )
    }

    def getDisplayLabel(objet: Rdf#Node) =
      nodeToString(getObjects(graph, objet, URI("urn:displayLabel")).headOption.getOrElse(Literal("")))

    def processTriple(triple: Rdf#Triple) = {
      val pred = triple.predicate;
      val objet = triple.objectt
      addLine( pred match {
        case dbo("startDate") =>
          "DTSTAMP:" + nodeToString(objet).replaceAll("-", "") + "\r\n" +
          "DTSTART:" + nodeToString(objet).replaceAll("-", "")
        case URI("http://purl.org/NET/c4dm/event.owl#agent") =>
          "ORGANIZER:" + getDisplayLabel(objet)
        case dbo("endDate")   => "DTEND:" + nodeToString(objet).replaceAll("-", "")
        case URI("urn:displayLabel")   => "SUMMARY:" + nodeToString(objet)
        case URI("http://purl.org/NET/c4dm/event.owl#place") =>
                                 "GEO:" + getDisplayLabel(objet)
        case rdfs.comment     => "DESCRIPTION:" + nodeToString(objet)
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