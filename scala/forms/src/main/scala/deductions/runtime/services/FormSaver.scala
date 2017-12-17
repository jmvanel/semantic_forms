package deductions.runtime.services

import java.net.URLDecoder

import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.sparql_cache.{SPARQLHelpers, TypeAddition}
import deductions.runtime.utils.{RDFHelpers, Timer, URIManagement, DatabaseChanges}
import org.w3.banana.{RDF, TryW}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Try}
import deductions.runtime.core.HTTPrequest
import scala.util.Success

import scalaz._
import Scalaz._

trait FormSaver[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with TypeAddition[Rdf, DATASET]
    with HttpParamsManager[Rdf]
//    with LogAPI[Rdf]
    with SaveListenersManager[Rdf]
    with RDFHelpers[Rdf]
    with SPARQLHelpers[Rdf, DATASET]
    with Timer
    with URIManagement {

  import ops._


  def computeDatabaseChanges(
    httpRequest: HTTPrequest): DatabaseChanges[Rdf] =
    computeDatabaseChangesFromMap(
        httpRequest.formMap,
        httpRequest.getLanguage() )

  private def computeDatabaseChangesFromMap(
    httpParamsMap: Map[String, Seq[String]],
    lang: String): DatabaseChanges[Rdf] = {
    var databaseChanges = DatabaseChanges[Rdf]()
    val comingBackTriples = getTriplesFromHTTPparams(httpParamsMap)
    comingBackTriples.foreach {
      comingBackTriple =>
        databaseChanges =
          computeDatabaseChangesFor1Triple(
            comingBackTriple._1,
            comingBackTriple._2 /* objects*/ ,
            databaseChanges, lang)
    }
    databaseChanges
  }
    
  def getTriplesFromHTTPrequest(httpRequest: HTTPrequest): Iterable[(Rdf#Triple, Seq[String])] = {
    getTriplesFromHTTPparams(httpRequest.formMap)
  }


  /** get Triples From HTTP parameters: decode HTTP parameters */
  private def getTriplesFromHTTPparams(queryString: Map[String, Seq[String]])
  : Iterable[(Rdf#Triple, Seq[String])] = {

    log(
      s"\n>>>>==== getTriplesFromHTTPparams: query map keys: ${queryString.keys}");

    val res = queryString.toSeq.map {
      // cf partial functions:  http://blog.bruchez.name/2011/10/scala-partial-functions-without-phd.html
      case (param0, objects0) =>
        if (isSpecialHTTPparameterForTriple(param0)) {
          val param = URLDecoder.decode(param0, "utf-8")
          val objects = objects0.map { node =>
            URLDecoder.decode(node, "utf-8") }
          log(s"getTriplesFromHTTPparams: httpParam decoded: $param - objects $objects");
          val tryTriple = Try {
            val comingBackTriple = httpParam2Triple(param)
            log(s"getTriplesFromHTTPparams: triple from httpParam: {$comingBackTriple} - objects $objects")
            comingBackTriple
          }
          if (tryTriple.isFailure) logger.error(s"getTriplesFromHTTPparams: ERROR: param $param : result $tryTriple")
          tryTriple match {
            case Success(triple) => (triple, objects)
            case Failure(f)      =>
              System.err.println(s"getTriplesFromHTTPparams: non foreseen case : $param0 -> $objects0 - $f")
              (Triple(nullURI, nullURI, nullURI), Seq())
          }
        } else {
          (Triple(nullURI, nullURI, nullURI), Seq())
        }
    }
    res
  }

  private def isSpecialHTTPparameterForTriple(param0: String) = (
        param0 != "url" &&
        param0 != "uri" &&
        param0 != "graphURI")

  /** save triples in named graph given by HTTP parameter "graphURI";
   *  other HTTP parameters are original triples in Turtle;
   *
   * Important HTTP parameters:
   * - uri: (main) subject URI
   * - graphURI: URI of named graph in which save triples will be saved
   * 
   * transactional
   * @param map a raw map of HTTP response parameters
   * @return main subject URI,
   *         type change flag */
  def saveTriples(httpParamsMap: Map[String, Seq[String]],lang: String = "")
      ( implicit userURI: String = "" ): ( Option[String], Boolean)
      = {
    log(s"FormSaver.saveTriples httpParamsMap $httpParamsMap")
    log(s"""saveTriples: userURI <$userURI>""" )
    val uriArgs = httpParamsMap.getOrElse("uri", Seq())
    val encodedSubjectUriOption = uriArgs.find { uri => uri != "" }
    val graphURIOption = httpParamsMap.getOrElse("graphURI", Seq()).headOption
    log(s"FormSaver.saveTriples uri encoded $encodedSubjectUriOption, graphURI $graphURIOption")

    // PENDING: require subject URI in parameter "uri" is probably not necessary (case of sub-forms and inverse triples)
    lazy val subjectUriOption = encodedSubjectUriOption match {
      case Some(subjectUri0) =>
        val subjectUri = URLDecoder.decode(subjectUri0, "utf-8")
        log(s"FormSaver.saveTriples subjectUri $subjectUri")

        // named graph in which to save:
        val graphURI =
          if (graphURIOption === Some("")) subjectUri
          else URLDecoder.decode(graphURIOption.getOrElse(subjectUri0), "utf-8")
 
        val databaseChanges = computeDatabaseChangesFromMap( httpParamsMap, lang)

        doSave(graphURI, databaseChanges)

//        { // TEST
//          wrapInReadTransaction {
//            val typesTriples = find(allNamedGraph, URI(subjectUri), rdf.typ, ANY).toList
//            println(s"==== saveTriples: typesTriples $typesTriples")
//          }
//        }
        ( Some(subjectUri), databaseChanges.typeChange)
      case _ => (None, false)
    }

    return subjectUriOption
  }

  /** process a single triple from the form
   *  @return augmented `databaseChanges` argument */
  private def computeDatabaseChangesFor1Triple(
      originalTriple: Rdf#Triple, objectsFromUser: Seq[String],
                             databaseChanges: DatabaseChanges[Rdf],
                             lang: String = ""): DatabaseChanges[Rdf] = {

    val triplesToAdd = ArrayBuffer[Rdf#Triple]()
    val triplesToRemove = ArrayBuffer[Rdf#Triple]()
    var typeChange = false
    
      log(s"computeDatabaseChanges: originalTriple: $originalTriple, objectsFromUser $objectsFromUser")
      objectsFromUser.map { objectStringFromUser =>
        // NOTE: a single element in objects
        val objectFromUser = foldNode(originalTriple.objectt)(
          _ => {
            if (objectStringFromUser.startsWith("_:"))
              BNode(objectStringFromUser.substring(2))
            else {
              if (objectStringFromUser != "")
                log(s"""computeDatabaseChanges: objectStringFromUser "$objectStringFromUser" changed: spaces removed""")
            URI(
              expandOrUnchanged(
                makeURIFromString(
                  objectStringFromUser,
                  fromUri(originalTriple.predicate))))
            }
          },
          _ => BNode(objectStringFromUser.replaceAll(" ", "_")), // ?? really do this ?
          _ =>
            // avoids that numbers get a language tag
            if ("[a-zA-Z]".r .findFirstMatchIn(objectStringFromUser) .isDefined )
        	  Literal.tagged(objectStringFromUser,Lang(lang))
          else Literal(objectStringFromUser)
        )
        val originalData = nodeToString(originalTriple.objectt)
        val emptyUserInput: Boolean = objectStringFromUser === ""
        val differingUserInput: Boolean = objectStringFromUser != originalData
        val originalDataNonEmpty: Boolean = originalData != ""
        val newUserInput: Boolean = !emptyUserInput && differingUserInput

        log(s""">> originalData $originalData,
          emptyUserInput $emptyUserInput, differingUserInput $differingUserInput, originalDataNonEmpty $originalDataNonEmpty, newUserInput $newUserInput,
          objectStringFromUser "$objectStringFromUser"""
          )

        if( !emptyUserInput && differingUserInput ||
            // NOTE: the case of pre-filled rdfs:type
            (originalTriple.predicate == rdf.typ &&
                !emptyUserInput))
          triplesToAdd +=
            makeTriple(originalTriple.subject, originalTriple.predicate, objectFromUser)

        if (originalDataNonEmpty && differingUserInput)
          triplesToRemove += originalTriple

        log("computeDatabaseChanges: predicate " + originalTriple.predicate + ", originalTriple.objectt: \"" +
          originalTriple.objectt.toString() + "\"" +
          s""", objectStringFromUser "$objectStringFromUser", triplesToRemove $triplesToRemove""")

        // FEATURE: change type triggers edit mode
        if (originalTriple.predicate == rdf.typ && differingUserInput) {
          println(s">>>> computeDatabaseChanges: typeChange! objectFromUser($objectFromUser)")
          typeChange = true
        }
      }
          
      databaseChanges.copy(
          triplesToAdd = databaseChanges.triplesToAdd ++ triplesToAdd,
          triplesToRemove = databaseChanges.triplesToRemove ++ triplesToRemove,
          typeChange = databaseChanges.typeChange || typeChange)
    }

  /** do Save the computed Database Changes - transactional */
  private def doSave(graphURI: String, databaseChanges: DatabaseChanges[Rdf])
    ( implicit userURI: String = graphURI ) {
    import databaseChanges._

    // DEBUG
    val dsg = dataset.asInstanceOf[org.apache.jena.sparql.core.DatasetImpl].asDatasetGraph()
    println(s">>>> doSave: dsg class : ${dsg.getClass}")
    println(s">>>> doSave: ds: ${dataset}")

      val transaction = wrapInTransaction {
        time("removeTriples",
          rdfStore.removeTriples( dataset,
            URI(graphURI),
            triplesToRemove.toIterable), logger.isDebugEnabled() )

          // special case of triples  ?S rdf:type foaf:Document.
          rdfStore.removeTriples( dataset,
            URI("user:anonymous"),
            triplesToRemove.toIterable)

        val res =
          time("appendToGraph",
            rdfStore.appendToGraph( dataset,
              URI(graphURI),
              makeGraph(triplesToAdd)), logger.isDebugEnabled())
        log( s"doSave: triplesToRemove ${triplesToRemove.mkString(", ")}")
        log( s"doSave: triplesToAdd ${triplesToAdd.mkString(", ")}")
        res
      }

      Future { wrapInTransaction {
    	  addTypes(triplesToAdd,
    			  Some(URI(graphURI)))
      } }

      implicit val rdfLocalProvider: RDFStoreLocalProvider[Rdf, _] = this
      /* TODO maybe: in the hook here: return the future to print later what has been done */
      callSaveListeners(triplesToAdd, triplesToRemove)

      val f = transaction.asFuture

      f onSuccess {
        case _ =>
          logger.info(s""" Successfully stored ${triplesToAdd.size} triples
            ${triplesToAdd.mkString(", ")}
            and removed ${triplesToRemove.size}
            triplesToRemove:
            ${triplesToRemove.mkString("\n")}
          in graph <$graphURI>""")
      }
      f.onFailure { case t => logger.error(s"doSave: Failure $t") }
    }

  private def log(s: String) =
//    println("FormSaver: "+s)
    logger.debug(s)

}

