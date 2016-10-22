package deductions.runtime.utils

import scala.collection.JavaConversions.asScalaIterator
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

import org.apache.jena.riot.RDFDataMgr
import org.w3.banana.RDF
import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule

//import com.hp.hpl.jena.rdf.model.ModelFactory
//import com.hp.hpl.jena.util.FileManager
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.util.FileManager

import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.sparql_cache.RDFCacheAlgo


/**
 * @author jmv
 */

/**
 * Fix Bad URI in N-Quads dump:
 *  From:
 *  <bad URI> <p> <bad Object> .
 *  To:
 *  <bad_URI> <p> <bad_Object> .
 *
 *  TODO: reuse a single function for fixing URI,
 *  common to TypeAddition
 */
object FixBadURIApp extends JenaModule
    with FixBadURI[Jena, ImplementationSettings.DATASET]
    with RDFStoreLocalJena1Provider
    //    with JenaHelpers 
    with App {
  fix
  def listGraphNames() = dataset.listNames()
}

trait FixBadURI[Rdf <: RDF, DATASET] extends RDFCacheAlgo[Rdf, DATASET]
    with RDFStoreLocalProvider[Rdf, DATASET] {

  val fileNameOrUri = "dump.nq";
  def listGraphNames(): Iterator[String]
  //  val corrections = scala.collection.mutable.Buffer[(Rdf#Triple, Rdf#Triple)]()
  val triples = ArrayBuffer[Rdf#Triple]()
  val triplesToRemove = ArrayBuffer[Rdf#Triple]()
  var corrections = 0

  import ops._
  import rdfStore.transactorSyntax._

  def fix() = {
    val names =
      dataset.r {
        listGraphNames
      }.get.toIterable
    System.err.println("names size " + names.size)

    for (uri <- names) {
      System.err.println("Graph name " + uri)
      val graphURI = URI(uri)
      if (fixURI(graphURI) == graphURI) {
        fixGraph(graphURI)
      } else {
        System.err.println("  Bad graph URI: " + graphURI)
        fixGraph(graphURI, fixURI(graphURI))
        rdfStore.removeGraph( dataset, graphURI)
      }
    }
    System.err.println("" + corrections + " corrections")
  }

  def fixGraph(graphURI: Rdf#URI) {
    fixGraph(graphURI, graphURI)
  }

  def fixGraph(graphURI: Rdf#URI, newGraphURI: Rdf#URI) {
    dataset.rw {
      val gr = rdfStore.getGraph( dataset, graphURI).get
      val trs = gr.triples
      for (tr <- trs) {
        val newTriple = fixTriple(tr)
        // System.err.println(tr)
        // System.err.println( "NEW " + newTriple)
        if (newTriple != tr) {
          System.err.println(newTriple)
          triplesToRemove += tr
          triples += newTriple
          corrections = corrections + 1
        }
      }
      try {
        rdfStore.removeTriples( dataset, newGraphURI, triplesToRemove.toIterable)
      } catch {
        case t: Throwable =>
          System.err.println("Could not remove Triples from " + s"<$newGraphURI>")
      }
      rdfStore.appendToGraph( dataset, newGraphURI, makeGraph(triples))
    }

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