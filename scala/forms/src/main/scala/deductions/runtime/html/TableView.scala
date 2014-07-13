package deductions.runtime.html

import org.w3.banana._
import org.w3.banana.jena.JenaModule
import org.w3.banana.RDFXMLReaderModule
import deductions.runtime.abstract_syntax.FormSyntaxFactory
import org.w3.banana.diesel._
import org.w3.banana.jena.Jena
import deductions.runtime.sparql_cache.RDFCacheJena
import org.apache.log4j.Logger
import scala.xml.Elem

/** Table View of a form */
trait TableView
  extends RDFModule
  with RDFOpsModule
  with TurtleReaderModule
  with RDFXMLReaderModule
  with RDFXMLWriterModule
  with JenaModule // TODO depend on generic Rdf
  with Form2HTML[Jena#URI]
  with RDFCacheJena // TODO depend on generic Rdf
{
  import Ops._
  
  val nullURI : Rdf#URI = Ops.URI( "" )

  val foaf = FOAFPrefix[Rdf]
  val foafURI = foaf.prefixIri

  /** create a form for given uri with background knowledge ??? TODO */
    def htmlFormString(uri:String) : String = {
    htmlForm(uri).toString
  }

  def htmlForm(uri:String) : Elem = {
    // TODO load ontologies from local SPARQL; probably use a pointed graph
    /* TODO use Jena Riot for smart reading of any format,
    cf https://github.com/w3c/banana-rdf/issues/105 */
    val from = new java.net.URL(uri).openStream()
//    val graph = RDFXMLReader.read(from, uri).get
    val graph = TurtleReader.read(from, uri).get
    graf2form(graph, uri)
  }

  /** create a form for given uri with background knowledge in graph1 */
  def graf2formString(graph1: Rdf#Graph, uri:String): String = {
    graf2form(graph1, uri).toString
  }

  def graf2form(graph1: Rdf#Graph, uri:String): Elem = {
    val connection = new java.net.URL(foafURI).openConnection()
    val acceptHeaderRDFXML = "application/rdf+xml"
    connection.setRequestProperty("Accept", acceptHeaderRDFXML)
    val from = connection.getInputStream()
    Logger.getRootLogger().info( s"Reading from $foafURI" )
//    val vocabGraph = TurtleReader.read(from, foafURI).get
    val vocabGraph = RDFXMLReader.read(from, foafURI).get

    val graph = graph1.union(vocabGraph)

    val factory = new FormSyntaxFactory[Rdf](graph)
    println((graph.toIterable).mkString("\n"))
    val form = factory.createForm(URI(uri),
      Seq(foaf.name))
    println("form:\n" + form)
    val htmlForm = generateHTML(form)
    println(htmlForm)
    htmlForm
//    val htmlFormString = htmlForm.toString
//    htmlFormString
  }
}