package deductions.runtime.jena.lucene

import org.apache.jena.query.text.{EntityDefinition, TextDatasetFactory}
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.store.NIOFSDirectory
//import org.apache.solr.client.solrj.SolrServer
//import org.apache.solr.client.solrj.impl.HttpSolrServer

import java.nio.file.Paths

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.utils.{Configuration, RDFPrefixes}
import org.w3.banana.{FOAFPrefix, Prefix, RDFOps, RDFSPrefix}
import org.apache.jena.query.text.TextIndex
import org.apache.jena.query.text.TextIndexConfig
import org.apache.jena.query.spatial.SpatialDatasetFactory
import org.apache.jena.query.DatasetFactory
import org.apache.jena.query.spatial.SpatialIndex
import org.apache.jena.query.spatial.SpatialQuery

/**
 * see https://jena.apache.org/documentation/query/text-query.html
 * see [[deductions.runtime.services.StringSearchSPARQL]] for search query
 */
trait LuceneIndex // [Rdf <: RDF]
    extends RDFPrefixes[ImplementationSettings.Rdf]
{
  val config: Configuration

  implicit val ops: RDFOps[ImplementationSettings.Rdf]

  /** cf trait InstanceLabelsInference */
  lazy val rdfIndexingBIG: EntityDefinition = {
    val rdfs = RDFSPrefix[ImplementationSettings.Rdf]
    val foaf = FOAFPrefix[ImplementationSettings.Rdf]

    /* this means: in Lucene the URI will be kept in key "uri",
     * the text indexed will be kept in key "text" */
    val entMap = new EntityDefinition("uri", "text", rdfs.label)
    entMap.setLangField("lang")
    entMap.setUidField("uid")
    // slows the query even on moderately large TDB (see thread on Jena list)
//    entMap.setGraphField("graph")

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

    entMap.set("text", sioc("content"))

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

  lazy val rdfIndexingSMALL: EntityDefinition = {
		  val entMap = new EntityDefinition("uri", "text", rdfs.label)
				  entMap
    }

  lazy val rdfIndexing = rdfIndexingBIG

  /** UNUSED - configure Lucene Index for Jena - TEST spatial Textual */
  private def configureLuceneIndexTESTspatial_Textual(dataset: ImplementationSettings.DATASET, useTextQuery: Boolean):
//  (ImplementationSettings.DATASET, TextIndex)
  ImplementationSettings.DATASET = {
    println(s"configureLuceneIndex: useTextQuery $useTextQuery")
    if (useTextQuery) {
      println(
          s"""configureLuceneIndex spatial + Textual by code: rdfIndexing getPredicates("text").size ${rdfIndexing.getPredicates("text").size}""")
        val directory = new NIOFSDirectory(Paths.get("LUCENE"))
        val textIndexConfig = new TextIndexConfig(rdfIndexing)
    	  textIndexConfig . setMultilingualSupport(true)
    	  val textIndex: TextIndex = TextDatasetFactory.createLuceneIndex(
    			  directory, textIndexConfig )

        val textualDataset = TextDatasetFactory.create(dataset, textIndex, true)

        val directorySpatial = new NIOFSDirectory(Paths.get("LUCENESpatial"))
        val entityDefinitionsSpatial = new org.apache.jena.query.spatial.EntityDefinition("entityField", "geoField")
        val spatialDataset = SpatialDatasetFactory.createLucene(textualDataset, directorySpatial, entityDefinitionsSpatial)

        return spatialDataset
    } else
      dataset
  }

  /** UNUSED - configure Lucene Index for Jena - TEST 2 spatial Textual:
   *  text query works, spatial NO */
  private def configureLuceneIndexTEST2spatial_Textual(dataset: ImplementationSettings.DATASET,
      useTextQuery: Boolean):
    ImplementationSettings.DATASET = {
    println(s"configureLuceneIndex: useTextQuery $useTextQuery")
    if (useTextQuery) {
      println(
          s"""configureLuceneIndex spatial + Textual by code 2: rdfIndexing getPredicates("text").size ${rdfIndexing.getPredicates("text").size}""")
      val directory = new NIOFSDirectory(Paths.get("LUCENE"))
      val textIndexConfig = new TextIndexConfig(rdfIndexing)
      textIndexConfig.setMultilingualSupport(true)
      val textIndex: TextIndex = TextDatasetFactory.createLuceneIndex(
        directory, textIndexConfig)
      val textualDataset = TextDatasetFactory.create(dataset, textIndex, true)

      val directorySpatial = new NIOFSDirectory(Paths.get("LUCENESpatial"))
      val entityDefinitionsSpatial = new org.apache.jena.query.spatial.EntityDefinition("entityField", "geoField")
      val spatialIndex: SpatialIndex =
        SpatialDatasetFactory.createLuceneIndex(directorySpatial, entityDefinitionsSpatial)
      textualDataset.getContext().set(SpatialQuery.spatialIndex, spatialIndex)
      return textualDataset
    } else
      return dataset
  }

  /** configure Lucene Index for Jena, with Jena assembler file */
  private def configureLuceneIndexAssembler(assemblerFile: String):
    ImplementationSettings.DATASET = {
       DatasetFactory.assemble( assemblerFile,
         "http://localhost/jena_example/#indexed-dataset")
  }

  def configureLuceneIndex(dataset: ImplementationSettings.DATASET,
        useTextQuery: Boolean,
        useSpatialIndex: Boolean):
  ImplementationSettings.DATASET = {
    configureLuceneIndexWithAssembler(dataset, useTextQuery, useSpatialIndex)
//    configureLuceneIndexTEST2spatial_Textual(dataset, useTextQuery)
  }

  def configureLuceneIndexWithAssembler(dataset: ImplementationSettings.DATASET,
        useTextQuery: Boolean,
        useSpatialIndex: Boolean):
  ImplementationSettings.DATASET = {    val assemblerFile =
      if (useTextQuery && useSpatialIndex)
        Some("jena.spatial+text2.assembler.ttl")
      else if (useTextQuery && !useSpatialIndex)
        Some("jena.text.assembler.ttl")
      else if (!useTextQuery && useSpatialIndex)
         Some("jena.spatial.assembler.ttl")
      else None // "jena.plain.assembler.ttl"
    println(s"configureLuceneIndex: assemblerFile: $assemblerFile")
    assemblerFile match {
      case Some(file) => configureLuceneIndexAssembler(file)
      case _ => dataset
    }
  }
}
