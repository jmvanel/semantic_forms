package deductions.runtime.sparql_cache.apps

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.io.Source
//import scala.collection.JavaConverters

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.utils.{DefaultConfiguration, URIHelpers}
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.util.FileManager
import org.w3.banana.RDF

import scalaz._
import Scalaz._

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
object FixBadURIApp extends  {
  override val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
} with ImplementationSettings.RDFModule
    with FixBadURI[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with ImplementationSettings.RDFCache // RDFStoreLocalJenaProvider
    with URIHelpers
    with App {
  
  def listGraphNames() = listNames(dataset)
  if( args.size === 0 )
    fix
  else
    fixGraph( ops.URI(args(0)) )
}

trait FixBadURI[Rdf <: RDF, DATASET]
		extends RDFCacheAlgo[Rdf, DATASET]
//    with RDFStoreLocalProvider[Rdf, DATASET]
{

  val fileNameOrUri = "dump.nq";
  def listGraphNames(): Iterator[String]
  private val triples = ArrayBuffer[Rdf#Triple]()
  private val triplesToRemove = ArrayBuffer[Rdf#Triple]()
  private var corrections = 0

  import ops._

  def fix() = {
    val names =
      wrapInReadTransaction {
        listGraphNames.toIterable.toSeq
      }.get
    // println("Graph names size " + names.size) // CAUTION: Exception here because of Blank node for graph name !?! 

    println(s"maxMemory ${Runtime.getRuntime.maxMemory()} bytes")
    
    for (uri <- names) {
      println(s"Graph name <$uri>")
      val graphURI = URI(uri)
      wrapInTransaction {
        if (fixURI(graphURI) == graphURI) {
          fixGraph(graphURI)
        } else {
          System.err.println(s"  Bad graph URI: <$graphURI>")
          fixGraph(graphURI, fixURI(graphURI))
          rdfStore.removeGraph(dataset, graphURI)
        }
      }
      println(s"\ttotalMemory, freeMemory ${(Runtime.getRuntime.totalMemory(),Runtime.getRuntime.freeMemory() )} bytes")
    }
    System.err.println("" + corrections + " corrections")
  }

  def fixGraph(graphURI: Rdf#URI) {
    fixGraph(graphURI, graphURI)
  }

  def fixGraph(graphURI: Rdf#URI, newGraphURI: Rdf#URI) {
//    rdfStore.rw(dataset, {
      val gr = rdfStore.getGraph(dataset, graphURI).get
      val trs = gr.triples
      for (tr <- trs) {
        val newTriple = fixTriple(tr)
        // System.err.println(tr)
        // System.err.println( "NEW " + newTriple)
        //        if (newTriple != tr ) {
        newTriple match {
          case Some(newTriple) =>
            if (compareTriples(tr, newTriple)) {
              triples += newTriple
            } else {
            	System.err.println(s"$tr => newTriple $newTriple")
            	triplesToRemove += tr
            	corrections = corrections + 1
            }
          case None =>
            System.err.println(s"$tr => Triple eliminated")
        }
      }
      try {
        rdfStore.removeTriples(dataset, newGraphURI, triplesToRemove.toIterable)
      } catch {
        case t: Throwable =>
          System.err.println("Could not remove Triples from " + s"<$newGraphURI>")
      }
      rdfStore.appendToGraph(dataset, newGraphURI, makeGraph(triples))
//    })
  }
  
  /** fix Triple: remove spaces;
   *  if not Absolute URI return None */
  def fixTriple(tr: Rdf#Triple): Option[Rdf#Triple] = {
    val subject = tr.subject
    val predicate = tr.predicate
    val objet = tr.objectt
    val objectFixed = fixNode(objet)
    if( objectFixed != objet )
      System.err.println(s"""objectFixed $objectFixed
          in triple $tr""")
    tr match {
      case tr if isAbsoluteURI(subject) =>
      val newSubject = fixNode(subject)
      val newObject = fixNode(objet)
      Some(Triple(newSubject, predicate, newObject))

      case tr if subject.isBNode =>
      Some(Triple(subject, predicate, fixNode(objet) ))

      case _ =>
      System.err.println(s"subject is NOT Absolute URI nor BN ($subject) !!!")
      None
    } 

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
  
  private def readJena2() = {
    val ds = RDFDataMgr.loadDataset(fileNameOrUri)
    println("Dataset " + ds)
    val names = ds.listNames()
    println("names " + names)
    for (n <- names.asScala) {
      println("name " + n)
      val model = ds.getNamedModel(n)
      println(model)
      val stats = model.listStatements()
      for (st <- stats.asScala) {
        val s = st.getSubject
        val p = st.getPredicate
        val o = st.getObject
        model.createStatement(s, p, o)
      }
    }
  }

  private def readJena = {
    val model = ModelFactory.createDefaultModel();
    val is = FileManager.get().open(fileNameOrUri);
    if (is != null) {
      model.read(is, null, "N-QUADS");
      model.write(System.out, "TURTLE");
    } else {
      System.err.println("cannot read " + fileNameOrUri); ;
    }
  }

  private def badAlgo() = {
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