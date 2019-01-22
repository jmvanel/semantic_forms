package deductions.runtime.abstract_syntax

import deductions.runtime.core.FormModule
import deductions.runtime.semlogs.TimeSeries
import org.w3.banana.RDF

import scala.collection.mutable
import scala.language.{existentials, postfixOps}

import scalaz._
import Scalaz._

/** TODO move to package user */
trait UserTraceability[Rdf <: RDF, DATASET]
  extends FormModule[Rdf#Node, Rdf#URI]
    with TimeSeries[Rdf, DATASET] {

  /** add User Info On Triples whose subject is form subject;
   *  for editing in /table view */
  def addUserInfoOnTriples(
                            formSyntax: FormSyntax)(
                            implicit graph: Rdf#Graph,
                            lang: String = "en") : FormSyntax = {
    try {
    logger.info("XXXXXXXXXXXXXXXX addUserInfoOnTriples")
    val metadata = getMetadataAboutSubject(formSyntax.subject)
    /* Map : (property,objet) -> userId */
    val resultsUser = mutable.Map[(String,String), String]()
    /* Map : (property,objet) -> timeElement */
    val resultsTimestamp = mutable.Map[(String,String), Long]()

    for (row: Seq[Rdf#Node] <- metadata){
      logger.info(row)

      // each row contains: property, object, timestamp, user
      val propertyId =0; val objectId=1; val timestampId=2; val userId=3;

      val timeElement  = rdfNodeToLong(row(timestampId))
      val property = row(propertyId).toString
      val objet = row(objectId).toString

      if(resultsTimestamp.contains( (property,objet)) ){
        logger.info(resultsTimestamp( (property,objet) )
        + (" < ")
        + (timeElement)
        + (" = ")
        + (resultsTimestamp((property,objet)) < timeElement))
        if(resultsTimestamp((property,objet)) < timeElement){
          resultsTimestamp += ( (property,objet) -> timeElement)
          resultsUser put ((property, objet), row(userId).toString)
        }
      }
      else{
        resultsTimestamp put ( (property,objet), timeElement)
        // PENDING : already done above:
        resultsUser put ((property, objet), row(userId).toString)
      }
    }
    for (elem <- resultsTimestamp){
    	logger.info("\t" + elem)
    }

    debug(s"YYYYYYYY Before add User Info\n${formSyntax.fields.mkString("\n")}\n")
    val entries =
      for (field: Entry <- formSyntax.fields) yield {
      if (resultsUser.contains( (field.property.toString, field.value.toString) ) ){
    	  debug(s"ZZZZ add User Info ${field.label} ${field.value}")
        field.copyEntry(
            fromMetadata = resultsUser( (field.property.toString, field.value.toString)),
            fromTimeMetadata = resultsTimestamp.getOrElse( (field.property.toString, field.value.toString), 0 ) )
      } else {
        addUserFromGraph(field)
      }
    }
    formSyntax . fields = entries
    formSyntax
    }
    catch {
      case t: Throwable =>
        logger.error( "addUserInfoOnTriples" + t.getLocalizedMessage )
        t.printStackTrace(System.err)
        return formSyntax
    }
  }

  /** TODO there is something smarter in Banana */
  private def rdfNodeToLong(node: Rdf#Node): Long = {
    val timeElementStr = node.toString
    val nodeWithoutType = timeElementStr.splitAt(timeElementStr.indexOf("^"))._1.replaceAll("\"","")
    if(nodeWithoutType == "")
      0
    else nodeWithoutType.toLong
  }

  /** add User Info On all Triples in given FormSyntax */
  def addUserInfoOnAllTriples(
    formSyntax: FormSyntax)(
    implicit
    graph: Rdf#Graph,
    lang:  String    = "en"): FormSyntax = {
    try {
      logger.info("XXXXXXXXXXXXXXXX addUserInfoOnAllTriples")
      val entries =
        for (field: Entry <- formSyntax.fields) yield {
          val metadata: List[Seq[Rdf#Node]] = getMetadataAboutTriple(
              field.subject, field.property, field.value, 100, 0)
          debug( s">>>> addUserInfoOnAllTriples field $field, metadata $metadata")
          for (row: Seq[Rdf#Node] <- metadata) yield {
            field.copyEntry(
              fromTimeMetadata = rdfNodeToLong(row(0)),
              fromMetadata = row(1).toString())
          }
        }
//      println( s">>>> addUserInfoOnAllTriples entries $entries")
      formSyntax.fields = entries.flatten
      formSyntax
    } catch {
      case t: Throwable =>
        logger.error("addUserInfoOnAllTriples" + t.getLocalizedMessage)
        t.printStackTrace(System.err)
        return formSyntax
    }
  }

  /** add User in form entry From main Graph;
   *  NOTE: the URI added is not necessarily a user, but if not,
   *  it will simply not match current user and the triple will not be editable. */
  private def addUserFromGraph(field: Entry): Entry = {
    try {
    val resMainDatabase = {
      if (nodeToString(field.value) =/= "") {
        val queryMainDatabase = s"""
      SELECT ?USER
      WHERE {
        GRAPH ?USER {
         <${field.subject}> <${field.property}> ${makeTurtleTerm(field.value)} . }
      } """
        debug(s"addUserFromGraph: queryMainDatabase $queryMainDatabase")
        sparqlSelectQueryVariables(
          queryMainDatabase,
          Seq("USER"),
          dataset)
      } else List(Seq())
    }
    debug(s"addUserFromGraph: resMainDatabase $resMainDatabase")
    val fieldWithUsers = for (
      row <- resMainDatabase;
      node <- row
    ) yield {
      debug(s"addUserFromGraph: ?USER node ${node.getClass()} $node from subject <${field.subject}>")
      field.copyEntry(fromMetadata = node.toString)
    }
    fieldWithUsers.headOption.getOrElse(field)
    } catch {
      case t: Throwable =>
        System.err.println( s"ERROR in addUserFromGraph($field)" )
        t.printStackTrace()
        field
    }
  }

  private def debug(s: String) = // println( "!!!! "+s) //
    logger.debug(s)
}