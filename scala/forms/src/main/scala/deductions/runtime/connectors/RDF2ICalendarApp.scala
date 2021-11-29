package deductions.runtime.connectors

import java.io.FileOutputStream
import deductions.runtime.connectors.icalendar.RDF2ICalendar
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.utils.DefaultConfiguration
import deductions.runtime.sparql_cache.SPARQLHelpers

/** simple App that takes all triples and formats the schema:Event in ICalendar format */
object RDF2ICalendarApp
  extends {
    override val config = new DefaultConfiguration {
      override val useTextQuery: Boolean = true // false
    }
  } with App
  with ImplementationSettings.RDFCache
  with RDF2ICalendar[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
  with SPARQLHelpers[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  wrapInReadTransaction {
    writeGraph(allNamedGraph)
  }

  def writeGraph(graph: Rdf#Graph) : Unit = {
    val outputFile = "from-RDF.ics"
    println(s"""Write $outputFile,
    # of triples ${graph.size()}""")
    val os = new FileOutputStream(outputFile)
    val iCalendar = graph2iCalendar(graph)
    os.write(iCalendar.getBytes)
    os.close()
    println(s"Writen $outputFile" )
  }
}
