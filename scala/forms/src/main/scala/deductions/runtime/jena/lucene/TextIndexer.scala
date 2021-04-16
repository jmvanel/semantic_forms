package deductions.runtime.jena.lucene

import scala.collection.JavaConverters.iterableAsScalaIterableConverter

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.utils.DefaultConfiguration

import org.apache.jena.query.text.DatasetGraphText
//import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import collection.JavaConverters._

import org.apache.jena.query.text.EntityDefinition
import org.apache.jena.graph.Node
import org.apache.jena.sparql.core.Quad
import org.apache.jena.query.text.TextQueryFuncs

/**
 * Index TDB with Lucene a posteriori;
 *  see https://jena.apache.org/documentation/query/text-query.html
 */
object TextIndexerRDF extends App {
  val te = new TextIndexerClass
  te.doIndex()
}

private[lucene] class TextIndexerClass extends // jena.textindexer() // Array[String]())
    ImplementationSettings.RDFModule
    with LuceneIndex {

  val config = new DefaultConfiguration {
    override val useTextQuery = true
  }

  val config1 = config
  // NOTE: this triggers configureLuceneIndex()
  val rdfStoreProvider = new ImplementationSettings.RDFCache {
    val config = config1
  }
  val dataset0 = rdfStoreProvider.dataset
  val datasetWithLuceneConfigured = dataset0

  val graphWithLuceneConfigured = datasetWithLuceneConfigured.asDatasetGraph()
    println("datasetWithLuceneConfigured.asDatasetGraph() getClass " + graphWithLuceneConfigured.getClass)
  val datasetGraphText: DatasetGraphText = graphWithLuceneConfigured.asInstanceOf[DatasetGraphText]

  // NOTE: formerly overrided jena.textindexer fields
  val dataset = datasetGraphText
  val textIndex = dataset.getTextIndex()
  println("textIndex.getDocDef.fields " + textIndex.getDocDef.fields())
  val entityDefinition = rdfIndexing

  def doIndex() = {
//    this.entityDefinition = rdfIndexing
    println( s"entityDefinition $entityDefinition \n" )
    println( "textIndex.getDocDef hashCode " + textIndex.getDocDef.hashCode() )
    println( "entityDefinition hashCode " + entityDefinition.hashCode() )
    println( "getIndexedProperties 1 size " + getIndexedProperties(entityDefinition).size + " " + getIndexedProperties(entityDefinition) )
    println( "getIndexedProperties 2 size " + + getIndexedProperties(textIndex.getDocDef).size + " " +
        getIndexedProperties(textIndex.getDocDef) )
    println( s"""entityDefinition.getPredicates("text") """ + entityDefinition.getPredicates("text"))
    exec()
  }

  // override
  def exec() = {
    val properties = getIndexedProperties(textIndex.getDocDef)

    // there are various strategies possible here
    // what is implemented is a first cut simple approach
    // currently - for each indexed property
    // list and index triples with that property
    // that way only process triples that will be indexed
    // but each entity may be updated several times

    for (property <- properties) {
      val quadIter = dataset.find(Node.ANY, Node.ANY, property, Node.ANY);
      //            for (; quadIter.hasNext(); )
      for (quad <- quadIter.asScala) {
        val quad2 = if (Quad.isDefaultGraph(quad.getGraph())) {
          // Need to use urn:x-arq:DefaultGraphNode for text indexing (JENA-1133)
          Quad.create(Quad.defaultGraphNodeGenerated,
            quad.getSubject(), quad.getPredicate(), quad.getObject());
        } else
          quad

        val entity = TextQueryFuncs.entityFromQuad(entityDefinition, quad);
        if (entity != null) {
//          entity.toString()
          textIndex.addEntity(entity)
          println(s"$quad => $entity")
        }
      }
    }

    textIndex.commit();
    textIndex.close();
    dataset.close();
    //        progressMonitor.close() ;
  }

  /** For printing */
  def getIndexedProperties(entityDefinition: EntityDefinition) = {
//    val r = 
      for (
      f <- entityDefinition.fields.asScala;
      _ = println(s"	field $f") ;
      p <- entityDefinition.getPredicates(f).asScala
//      _ = println(s"	Predicate $p")
    ) yield p
//    r.toList
  }
}
