package deductions.runtime.jena.lucene

import deductions.runtime.utils.RDFPrefixes
import org.apache.jena.query.text.TextIndexLucene
import org.apache.jena.query.text.TextIndexException
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.document.Document
import scala.collection.JavaConversions._
import org.apache.jena.query.text.DatasetGraphText
import deductions.runtime.jena.ImplementationSettings
import scala.collection.Seq
import deductions.runtime.jena.RDFCache
import deductions.runtime.utils.DefaultConfiguration
import org.apache.lucene.search.ScoreDoc

/** Test Lucene Text Index */
object TestTextIndex extends {
  override val config = new DefaultConfiguration {
    override val useTextQuery = true
  }
} with App
    with ImplementationSettings.RDFCache
    with RDFPrefixes[ImplementationSettings.Rdf] {

  import ops._

  val pred1 = rdfs.label
  val pred2 = foaf.givenName

  rdfStore.rw(dataset, {
    rdfStore.appendToGraph(dataset, URI("test:/test"),
      makeGraph(
        Seq(Triple(URI("test:/test1"), pred1, Literal("test1")))))

    rdfStore.appendToGraph(dataset, URI("test:/test"),
      makeGraph(
        Seq(Triple(URI("test:/test2"), pred2, Literal("test1")))))
  })
  getTextIndex() match {
    case Some(textIndex) =>
      println(s"dump(textIndex=$textIndex)")
      dump(textIndex)
    case _ =>
  }

  /** pasted from jena.textindexdump */
  def dump(textIndex: TextIndexLucene) = {
    try {
      val directory = textIndex.getDirectory()
      val analyzer = textIndex.getQueryAnalyzer()
      val indexReader = DirectoryReader.open(directory)
      val indexSearcher = new IndexSearcher(indexReader)
      val queryParser = new QueryParser(
        textIndex.getDocDef().getPrimaryField(), analyzer)
      val query = queryParser.parse("*:*")
      val sDocs = indexSearcher.search(query, 1000).scoreDocs

      def printScoreDocs(sDocs: Array[ScoreDoc]) = {
        for (sd <- sDocs) {
          println("Doc: " + sd.doc);
          val doc: Document = indexSearcher.doc(sd.doc);
          // Don't forget that many fields aren't stored, just indexed.
          var i = 0
          for (f <- doc) {
            i = i + 1
            println(s"  $i $f");
            println("  " + f.name() + " = " + f.stringValue())
            //              f.fieldType() )
          }
        }
      }
      printScoreDocs(sDocs)

      println("search test1")
      val query1 = queryParser.parse("test1")
      val res = indexSearcher.search(query1, 1000).scoreDocs
      printScoreDocs(res)
    } catch {
      case ex: Throwable =>
        ex.printStackTrace()
        throw new TextIndexException(ex);
    }
  }

  def getTextIndex(): Option[TextIndexLucene] = {
    val dsg = dataset.asDatasetGraph()
    println(s"dsg class : ${dsg.getClass}")
    dsg match {
      case dsgText: DatasetGraphText =>
        println(s"case dsgText : ${dsg.getClass}")
        val textIndex = dsgText.getTextIndex
        println(s"textIndex class : ${textIndex.getClass}")
        textIndex match {
          case textIndex: TextIndexLucene => Some(textIndex)
          case _                          => None
        }
      case _ => None
    }
  }
}
