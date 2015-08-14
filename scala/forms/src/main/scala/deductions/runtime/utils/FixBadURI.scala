package deductions.runtime.utils

import scala.io.Source
import java.io.FileInputStream
import com.hp.hpl.jena.rdf.model.ModelFactory
import com.hp.hpl.jena.util.FileManager
import org.apache.jena.riot.RDFDataMgr
import scala.collection.JavaConversions._
import deductions.runtime.jena.RDFCache
import org.w3.banana.jena.JenaModule
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.sparql_cache.RDFCacheAlgo
import org.w3.banana.RDF
import deductions.runtime.dataset.RDFStoreLocalProvider
import org.w3.banana.jena.Jena
import com.hp.hpl.jena.query.Dataset
import deductions.runtime.jena.JenaHelpers
import scala.collection.mutable.ArrayBuffer

/**
 * @author jmv
 */

/**
 * Fix Bad URI in N-Quads dump:
 *  From:
 *  <bad URI> <p> <bad Object> .
 *  To:
 *  <bad_URI> <p> <bad_Object> .
 */
object FixBadURIApp extends JenaModule
    with FixBadURI[Jena, Dataset]
    with RDFStoreLocalJena1Provider
    with JenaHelpers with App {
  fix
  def listGraphNames() = dataset.listNames()
}

trait FixBadURI[Rdf <: RDF, Dataset] extends RDFCacheAlgo[Rdf, Dataset]
    with RDFStoreLocalProvider[Rdf, Dataset] {

  val fileNameOrUri = "dump.nq";
  def listGraphNames(): Iterator[String]
  //  val corrections = scala.collection.mutable.Buffer[(Rdf#Triple, Rdf#Triple)]()
  val triples = ArrayBuffer[Rdf#Triple]()
  val triplesToRemove = ArrayBuffer[Rdf#Triple]()

  import ops._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._
  import rdfStore.sparqlEngineSyntax._
  import scala.concurrent.ExecutionContext.Implicits.global

  def fix() = {
    val names =
      dataset.r {
        listGraphNames
      }.get.toIterable
    System.err.println("names size " + names.size)

    var corrections = 0
    for (uri <- names) {
      System.err.println("Graph name " + uri)
      val graphURI = URI(uri)
      if (fixURI(graphURI) == graphURI) {
        dataset.rw {
          val gr = dataset.getGraph(URI(uri)).get
          val trs = gr.triples
          for (tr <- trs) {
            val newTriple = fixTriple(tr)
            //            System.err.println(tr)
            //            System.err.println( "NEW " + newTriple)
            if (newTriple != tr) {
              System.err.println(newTriple)
              triplesToRemove += tr
              triples += newTriple
              corrections = corrections + 1
            }
          }
          dataset.removeTriples(graphURI, triplesToRemove.toIterable)
          dataset.appendToGraph(graphURI, makeGraph(triples))
        }
      } else
        System.err.println("  Bad graph URI: " + graphURI)
    }
      System.err.println("" + corrections + " corrections")
  }

  def fixTriple(tr: Rdf#Triple): Rdf#Triple = {
    val subject = tr.subject
    val predicate = tr.predicate
    val objet = tr.objectt
    val newSubject = fixNode(subject)
    val newObject = fixNode(objet)
    Triple(newSubject, predicate, newObject)
  }

  def fixNode(n: Rdf#Node) = {
    foldNode(n)(
      uri => fixURI(uri),
      _ => n,
      _ => n)
  }
  def fixURI(uri: Rdf#URI) = {
    val underscore = "_"
    URI(uri.toString().replaceAll(" ", underscore))
  }

  
  
  //// unused below
  
  def readJena2 = {
    val ds = RDFDataMgr.loadDataset(fileNameOrUri)
    println("Dataset " + ds)
    val names = ds.listNames()
    println("names " + names)
    for (n <- names) {
      println("name " + n)
      val model = ds.getNamedModel(n)
      println(model)
      val stats = model.listStatements()
      for (st <- stats) {
        val s = st.getSubject
        val p = st.getPredicate
        val o = st.getObject
        model.createStatement(s, p, o)
      }
    }
  }

  def readJena = {
    val model = ModelFactory.createDefaultModel();
    val is = FileManager.get().open(fileNameOrUri);
    if (is != null) {
      model.read(is, null, "N-QUADS");
      model.write(System.out, "TURTLE");
    } else {
      System.err.println("cannot read " + fileNameOrUri); ;
    }
  }

  def badAlgo = {
    var corrections = 0
    for (line <- Source.fromFile("dump.nq").getLines()) {
      val parts = line.split(" +")
      val subjet = parts(0)
      val predicate = parts(1)
      val objet = parts(2)
      val graph = parts(3)
      if (subjet.contains(" ") ||
        (objet.startsWith("<") &&
          objet.contains(" "))) {
        System.err.println("# " + line)
        corrections = corrections + 1
      }
      val underscore = "_"
      println(
        subjet.replaceAll(" ", underscore) + " " +
          predicate + " " +
          (if (objet.startsWith("<"))
            objet.replaceAll(" ", underscore)
          else objet)
          + " " + graph)
    }
    System.err.println("" + corrections + " corrections")
  }
}