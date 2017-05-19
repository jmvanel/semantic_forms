package deductions.runtime.jena.lucene

import java.io.File

import org.apache.jena.query.text.EntityDefinition
import org.apache.jena.query.text.TextDatasetFactory
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.store.NIOFSDirectory
import org.apache.lucene.util.Version
//import org.apache.solr.client.solrj.SolrServer
//import org.apache.solr.client.solrj.impl.HttpSolrServer

import org.w3.banana.FOAFPrefix
import org.w3.banana.RDFOps
import org.w3.banana.RDFSPrefix

import deductions.runtime.services.Configuration
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.utils.RDFPrefixes
import org.w3.banana.Prefix
import java.nio.file.Paths

/**
 * see https://jena.apache.org/documentation/query/text-query.html
 * see [[StringSearchSPARQL]] for search query
 */
trait LuceneIndex // [Rdf <: RDF]
    extends RDFPrefixes[ImplementationSettings.Rdf]
{
  val config: Configuration
  import config._

  implicit val ops: RDFOps[ImplementationSettings.Rdf]
  import ops._

  /** cf trait InstanceLabelsInference */
  val rdfIndexing: EntityDefinition = {
    val rdfs = RDFSPrefix[ImplementationSettings.Rdf]
    val foaf = FOAFPrefix[ImplementationSettings.Rdf]
    val entMap = new EntityDefinition("uri", "text", rdfs.label)
    entMap.set("text", foaf.givenName)
    entMap.set("text", foaf.familyName)
    entMap.set("text", foaf.firstName)
    entMap.set("text", foaf.lastName)
    entMap.set("text", foaf.name)
    entMap.set("text", rdfs.comment)

    entMap.set("text", dbo("abstract"))
    entMap.set("text", skos("prefLabel"))
    entMap.set("text", skos("altLabel"))
    entMap.set("text", skos("hiddenLabel"))

    entMap.set("text", dwc("genus"))
    entMap.set("text", dwc("specificEpithet"))

    // for Grands Voisins:
    lazy val gvoi = Prefix[ImplementationSettings.Rdf]("gvoi",
        "http://assemblee-virtuelle.github.io/grands-voisins-v2/gv.owl.ttl#" )
    entMap.set("text", dc("subject"))
    entMap.set("text", gvoi("administrativeName"))
    entMap.set("text", gvoi("proposedContribution"))
    entMap.set("text", gvoi("realisedContribution"))
    entMap.set("text", gvoi("building"))
    entMap.set("text", foaf.status)

    entMap
  }

  /** configure Lucene or SOLR Index for Jena */
  def configureLuceneIndex(dataset: ImplementationSettings.DATASET, useTextQuery: Boolean): ImplementationSettings.DATASET = {
    println(s"configureLuceneIndex: useTextQuery $useTextQuery")
    //    println(s"configureLuceneIndex: ${Thread.currentThread().getStackTrace().slice(0, 15).mkString("\n")}")
    if (useTextQuery) {
      /* this means: in Lucene the URI will be kept in key "uri",
         * the text indexed by SORL will be kept in key "text" */
//      if (solrIndexing) {
//        val server: SolrServer = new HttpSolrServer("http://localhost:7983/new_core")
//        // val pingResult = server.ping; println("pingResult.getStatus " + pingResult.getStatus) // 7983
//        TextDatasetFactory.createSolrIndex(dataset, server, rdfIndexing)
//      } else {
        val directory = new NIOFSDirectory(
            // new java.nio.file.Path
//            File
            Paths.get
            ("LUCENE"))
        TextDatasetFactory.createLucene(dataset, directory, rdfIndexing,
          new StandardAnalyzer())
//              Version.
////              LUCENE_4_9))
//              LUCENE_6_4_1 ))
//      }
    } else
      dataset

  }
}