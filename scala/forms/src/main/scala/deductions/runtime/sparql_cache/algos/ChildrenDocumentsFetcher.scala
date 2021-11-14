package deductions.runtime.sparql_cache.algos

import java.net.URL
import java.nio.file.StandardOpenOption

import deductions.runtime.utils.RDFPrefixes
import org.w3.banana.io.{NTriples, RDFLoader, RDFWriter}
import org.w3.banana.{RDF, RDFOps}

import scala.util.Try

trait ChildrenDocumentsFetcher[Rdf <: RDF]
extends RDFPrefixes[Rdf] {
  implicit val rdfLoader: RDFLoader[Rdf, Try]

  implicit val ops: RDFOps[Rdf]
  import ops._

  implicit val ntriplesWriter: RDFWriter[Rdf, Try, NTriples]

  
  /** fetch documents ?D such that:
   *  <url> ?P ?D .
   * where ?P is in list propertyFilter */
  def fetch(url: URL, propertyFilter: List[Rdf#URI]): List[Rdf#Graph] = {
    val graph = rdfLoader.load(url).getOrElse(emptyGraph)
    val v = getTriples(graph) collect {
      case triple if (propertyFilter.contains(triple.predicate) || propertyFilter.isEmpty) =>
        val doc = rdfLoader.load(new URL(triple.objectt.toString())).getOrElse(emptyGraph)
        println( s"Fetched object from triple $triple, size ${doc.size}" )
        doc
    }
    v.toList
  }

  def fetch(url: URL, propertyFilter: List[Rdf#URI],
            propertyFilter2: List[Rdf#URI], langWanted: String): List[Rdf#Triple] = {
    val grs = fetch(url, propertyFilter)

    val trss = for {
      gr <- grs
      pr <- propertyFilter2
      trs = find(gr, ANY, pr, ANY)
    } yield trs
    val triples = trss.flatten
    // filter on lang
    triples.filter { tr =>
      foldNode(tr.objectt)(
        _ => false,
        _ => false,
        literal => {
          val optionLang = fromLiteral(literal)._3
          optionLang match {
            case Some(lang) => lang == makeLang(langWanted)
            case _          => false
          }
        })
    }
  }


  /** will be used for machine learning */
  def fetchDBPediaAbstractFromInterestsAndExpertise(url: URL, lang: String="fr" ): List[Rdf#Triple] = {
    fetch(url, List(foaf.topic_interest, cco("cco:expertise")), List(rdfs.label, dbo("abstract")), lang)
  }
  
  def writeToNTriplesFile( triples: List[Rdf#Triple], file: String = "dump1.nt", base: String = "urn:sample" ) = {
	  import java.nio.charset.StandardCharsets
	  import java.nio.file.{Files, Paths}
	  val writer = Files.newBufferedWriter( Paths.get(file), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW )
    for( tr <- triples ) {
      writer.write( ntriplesWriter.asString( Graph(triples), Some(base) ).get )
    }
	  writer.close()
  }
}
