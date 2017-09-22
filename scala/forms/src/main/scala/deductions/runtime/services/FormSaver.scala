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
    comingBackTriples.foreach { comingBackTriple =>
      databaseChanges =
        computeDatabaseChangesFor1Triple(
          comingBackTriple._1,
          comingBackTriple._2 /* objects*/ , databaseChanges, lang)
    }
    //    httpParamsMap.map {
    //      case (param0, objects) =>
    //        val param = URLDecoder.decode(param0, "utf-8")
    //        logger.debug(s"\nsaveTriples: httpParam decoded: $param")
    //        if (param != "url" &&
    //          param != "uri" &&
    //          param != "graphURI") {
    //          val try_ = Try {
    //            val comingBackTriple = httpParam2Triple(param)
    //            logger.debug(s"saveTriples: triple from httpParam: {$comingBackTriple }")
    //            databaseChanges = computeDatabaseChangesFor1Triple(comingBackTriple, objects, databaseChanges, lang)
    //          }
    //          try_ match {
    //            case f: Failure[_] => logger.error(s"saveTriples: $param :" + f)
    //            case _             =>
    //          }
    //        }
    //    }
    databaseChanges
  }
    
  def getTriplesFromHTTPrequest(httpRequest: HTTPrequest): Iterable[(Rdf#Triple, Seq[String])] = {
    getTriplesFromHTTPparams(httpRequest.formMap)
  }

  private def getTriplesFromHTTPparams(queryString: Map[String, Seq[String]])
  : Iterable[(Rdf#Triple, Seq[String])] = {
    val res = queryString.map {
      // cf partial functions:  http://blog.bruchez.name/2011/10/scala-partial-functions-without-phd.html
      case (param0, objects) if (
        param0 != "url" &&
        param0 != "uri" &&
        param0 != "graphURI") =>
        val param = URLDecoder.decode(param0, "utf-8")
        logger.debug(s"\ngetTriplesFromHTTPparams: httpParam decoded: $param");
        val tryTriple = Try {
          val comingBackTriple = httpParam2Triple(param)
          logger.debug(s"getTriplesFromHTTPparams: triple from httpParam: {$comingBackTriple }")
          comingBackTriple
        }
        if (tryTriple.isFailure) logger.error(s"getTriplesFromHTTPparams: $param : $tryTriple")
        tryTriple match {
          case Success(triple) => (triple, objects)
          case _ => (Triple(nullURI, nullURI, nullURI), Seq())
        }
      case x =>
        println(s"getTriplesFromHTTPparams: _ : $x")
        (Triple(nullURI, nullURI, nullURI) , Seq() )
    }
    res
  }

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
    logger.debug(s"FormSaver.saveTriples httpParamsMap $httpParamsMap")
    logger.debug(s"""saveTriples: userURI <$userURI>""" )
    val uriArgs = httpParamsMap.getOrElse("uri", Seq())
    val encodedSubjectUriOption = uriArgs.find { uri => uri != "" }
    val graphURIOption = httpParamsMap.getOrElse("graphURI", Seq()).headOption
    logger.debug(s"FormSaver.saveTriples uri encoded $encodedSubjectUriOption, graphURI $graphURIOption")

    // PENDING: require subject URI in parameter "uri" is probably not necessary (case of sub-forms and inverse triples)
    lazy val subjectUriOption = encodedSubjectUriOption match {
      case Some(subjectUri0) =>
        val subjectUri = URLDecoder.decode(subjectUri0, "utf-8")
        logger.debug(s"FormSaver.saveTriples subjectUri $subjectUri")

        // named graph in which to save:
        val graphURI =
          if (graphURIOption == Some("")) subjectUri
          else URLDecoder.decode(graphURIOption.getOrElse(subjectUri0), "utf-8")
 
        val databaseChanges = computeDatabaseChangesFromMap( httpParamsMap, lang)

        doSave(graphURI, databaseChanges)
        ( Some(subjectUri), databaseChanges.typeChange)
      case _ => (None, false)
    }

    return subjectUriOption
  }

  /** process a single triple from the form */
  private def computeDatabaseChangesFor1Triple(originalTriple: Rdf#Triple, objectsFromUser: Seq[String],
                             databaseChanges: DatabaseChanges[Rdf],
                             lang: String = ""): DatabaseChanges[Rdf] = {

    val triplesToAdd = ArrayBuffer[Rdf#Triple]()
    val triplesToRemove = ArrayBuffer[Rdf#Triple]()
    var typeChange = false
    
      //      if (originalTriple.objectt == foaf.Document ) // predicate == foaf.firstName) logger.debug( "DDDDDDDDDDD "+ foaf.Document)
      logger.debug(s"computeDatabaseChanges: originalTriple: $originalTriple, objectsFromUser $objectsFromUser")
      objectsFromUser.map { objectStringFromUser =>
        // NOTE: a single element in objects
        val objectFromUser = foldNode(originalTriple.objectt)(
          _ => {
            if (objectStringFromUser.startsWith("_:"))
              BNode(objectStringFromUser.substring(2))
            else {
              if (objectStringFromUser != "")
                logger.debug(s"""computeDatabaseChanges: objectStringFromUser "$objectStringFromUser" changed: spaces removed""")
              URI( makeURIFromString(objectStringFromUser,
                  fromUri(originalTriple.predicate)) )
            }
          },
          _ => BNode(objectStringFromUser.replaceAll(" ", "_")), // ?? really do this ?
          _ => Literal.tagged(objectStringFromUser,Lang(lang)))

        val originalData = nodeToString(originalTriple.objectt)
        val emptyUserInput: Boolean = objectStringFromUser == ""
        val differingUserInput: Boolean = objectStringFromUser != originalData
        val originalDataNonEmpty: Boolean = originalData != ""
        val newUserInput: Boolean = !emptyUserInput && differingUserInput

        logger.debug(s""">> originalData $originalData, emptyUserInput $emptyUserInput, differingUserInput $differingUserInput, originalDataNonEmpty $originalDataNonEmpty, newUserInput $newUserInput, objectStringFromUser $objectStringFromUser""")

        if( !emptyUserInput && differingUserInput ||
            // NOTE: the case of pre-filled rdfs:type
            originalTriple.predicate == rdf.typ)
          triplesToAdd +=
            makeTriple(originalTriple.subject, originalTriple.predicate, objectFromUser)

        if (originalDataNonEmpty && differingUserInput)
          triplesToRemove += originalTriple

        logger.debug("computeDatabaseChanges: predicate " + originalTriple.predicate + ", originalTriple.objectt: \"" +
          originalTriple.objectt.toString() + "\"" +
          ", objectStringFromUser \"" + objectStringFromUser + "\"")

        // FEATURE: change type triggers edit mode
        if (originalTriple.predicate == rdf.typ && differingUserInput) {
          println(s">>>> computeDatabaseChanges: typeChange! ($objectFromUser)")
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
        val res =
          time("appendToGraph",
            rdfStore.appendToGraph( dataset,
              URI(graphURI),
              makeGraph(triplesToAdd)), logger.isDebugEnabled())
        logger.debug( s"doSave: triplesToAdd ${triplesToAdd.mkString(", ")}")
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
}

