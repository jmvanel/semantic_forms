package deductions.runtime.jena.lucene

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.collection._

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
    logger.info("datasetWithLuceneConfigured.asDatasetGraph() getClass " + graphWithLuceneConfigured.getClass)
  val datasetGraphText: DatasetGraphText = graphWithLuceneConfigured.asInstanceOf[DatasetGraphText]

  // NOTE: formerly overrided jena.textindexer fields
  val dataset = datasetGraphText
  val textIndex = dataset.getTextIndex()
  logger.info("textIndex.getDocDef.fields " + textIndex.getDocDef.fields())
  val entityDefinition = rdfIndexing

  def doIndex() = {
//    this.entityDefinition = rdfIndexing
    logger.info( s"entityDefinition $entityDefinition \n" )
    logger.info( "textIndex.getDocDef hashCode " + textIndex.getDocDef.hashCode() )
    logger.info( "entityDefinition hashCode " + entityDefinition.hashCode() )
    logger.info( "getIndexedProperties 1 size " + getIndexedProperties(entityDefinition).size + " " + getIndexedProperties(entityDefinition) )
    logger.info( "getIndexedProperties 2 size " + + getIndexedProperties(textIndex.getDocDef).size + " " +
        getIndexedProperties(textIndex.getDocDef) )
    logger.info( s"""entityDefinition.getPredicates("text") """ + entityDefinition.getPredicates("text"))
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

    val graphs = mutable.Set[String]()
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

        val entity = TextQueryFuncs.entityFromQuad(entityDefinition, quad2);

        if (entity != null) {
          textIndex.addEntity(entity)
          logger.debug(s"$quad2 => $entity")
          if( graphs.add(quad2.getGraph().getURI() ) ) logger.info( s"""Graph ${quad2.getGraph()}""")
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
      _ = logger.info(s"	field $f") ;
      p <- entityDefinition.getPredicates(f).asScala
//      _ = logger.info(s"	Predicate $p")
    ) yield p
//    r.toList
  }
}
