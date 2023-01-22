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


    def processSubject(subject: Rdf#Node) = {
      def hasProperty(prop: Rdf#Node) = {
        val triples = find(graph, subject, prop, ANY).toList
        triples.size > 0 &&
        nodeToString( triples(0).objectt ) .length() >= 8
      }
      // No output for events without date !

//      if ( subject.toString() .endsWith("1611" ) ) {
//        println(s"" + find(graph, subject, prop, ANY).mk)
//      }
      if (hasProperty(dbo("startDate")) ||
        hasProperty(schema("doorTime"))) {

        addLine("BEGIN:VEVENT")
        addLine("UID:" + subject)
        for (
          triple2 <- find(graph, subject, ANY, ANY)
        ) { processTriple(triple2) }
        addLine("END:VEVENT")
      }
    }

    def getDisplayLabel(objet: Rdf#Node, prop: Rdf#URI = URI("urn:displayLabel")) = {
//      println(s"getDisplayLabel: objet $objet ${getObjects(allNamedGraph, objet, URI("urn:displayLabel")).toList}")
      nodeToString(getObjects(allNamedGraph, objet, prop).toList.headOption.getOrElse(Literal("")))
    }

    def formatDateTime( objet: Rdf#Node): String = {
      val value = nodeToString(objet)
      if( value == "")
        return ""
      val t0 = value.replaceAll("-", "").replaceAll(":", "")
      // "2013-09-18T14:30:00+02:00" : cut string after character "+" (otherwise not accepted by online validator)
      val t1 = t0.split( """\+""" )(0)
      val t2 = if ( t1 . contains("T"))
        if ( (t1.split("T"))(1) . length() == 4 )
          t1 + "00"
        else
          t1
      else
        t1 + "T120000"
      t2
    }

    def formatText(t: String): String =
      t
      .replaceFirst("\n$" ,"")
      .grouped(60)
      .map( s => s.replace("\n", "\\n") )
      .mkString("", CRLF+" ", "")

    def processTriple(triple: Rdf#Triple) = {
      val pred = triple.predicate;
      val objet = triple.objectt
      // println(s"processTriple( $triple")
      addLine( pred match {
        case dbo("startDate") => "DTSTAMP:" + formatDateTime( objet ) + "\n" +
                                 "DTSTART:" + formatDateTime( objet )
        case schema("doorTime") => "DTSTART:" + formatDateTime( objet )
        case URI("http://purl.org/NET/c4dm/event.owl#agent") =>
          "ORGANIZER:" + getDisplayLabel(objet).replace(' ', '_')
        case dbo("endDate")   => "DTEND:" + formatDateTime( objet ) // nodeToString(objet).replaceAll("-", "")
        case URI("urn:displayLabel") => formatText("SUMMARY:" + nodeToString(objet))
        case rdfs.label              => formatText("SUMMARY:" + nodeToString(objet))
        case URI("http://purl.org/NET/c4dm/event.owl#place") =>
                                 "LOCATION:" + getDisplayLabel(objet)
        case rdfs.comment     => formatText("DESCRIPTION:" + nodeToString(objet))
        case dct("abstract")  => formatText("DESCRIPTION:" + nodeToString(objet))
        case con("participant") => {
          if (objet != nullURI) {
            val mbox = getDisplayLabel(objet, foaf.mbox)
            val cn = getDisplayLabel(objet)
            val text =
              if (mbox != "")
                "ATTENDEE;CN=" + cn + ":MAILTO:" + mbox.replaceFirst("^mailto:", "")
              else if (cn != "")
                "ATTENDEE:CN=" + cn
              else ""
            formatText(text)
          } else ""
        }
        case _                => ""
      })
    }

    for ( triple <- eventTriples) {
        processSubject(triple.subject);
    }

    return "BEGIN:VCALENDAR\r\n" +
      "VERSION:2.0\r\n" +
      "PRODID:https://github.com/jmvanel/semantic_forms/wiki\r\n" +
      "CALSCALE:GREGORIAN\r\n" +
      "METHOD:PUBLISH\r\n" +
      "X-WR-TIMEZONE:Europe/Paris\r\n" +
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
LOCATION:48.85299;2.36885
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
