package deductions.runtime.services

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import org.w3.banana.FOAFPrefix
import org.w3.banana.RDF
import deductions.runtime.sparql_cache.RDFCacheAlgo
import scala.util.Failure

/**
 * @author jmv
 */
trait Authentication[Rdf <: RDF, DATASET] extends RDFCacheAlgo[Rdf, DATASET] {

  def passwordsGraph: Rdf#MGraph

  import ops._
  import rdfStore.transactorSyntax._

  val foaf = FOAFPrefix[Rdf]
  val passwordPred = URI("urn:password")

  /** compare password with database; @return user URI if success */
  def checkLogin(loginName: String, password: String): Boolean = {
    val databasePasswordOption = findPassword(loginName)
    databasePasswordOption match {
      case Some(databasePassword) => databasePassword == password
      case None => false
    }
  }

  /**
   * a user is a resource URI having user role in the application
   * (typically a foaf:Person),
   *  and having a password triple in dedicated Authentication database.
   */
  def findUser(loginName: String): Option[String] = {
    val databasePasswordOption = findPassword(loginName)
    databasePasswordOption match {
      case Some(databasePassword) => Some(loginName)
      case None => None
    }
  }

  private def findPassword(userid: String): Option[(String)] = {
    // query for password in dedicated Authentication database
    val userURI = URI(userid)

    val pwds = dataset.r({
      println( s"find( makeIGraph($passwordsGraph), $userURI, passwordPred, ANY)" )
      find( makeIGraph(passwordsGraph), userURI, passwordPred, ANY)
    }).get
    
    val pwdsl = pwds.toList
    println( s"pwdsl $pwdsl" )
    /* find( makeIGraph( {devil@hell.com @urn:password "bla"}), devil@hell.com, passwordPred, ANY)
       pwdsl List(devil@hell.com @urn:password "bla") */
    if (pwdsl.size > 0) {
      val databasePasswordNode = pwdsl(0).subject
      println(s"findUserAndPassword $databasePasswordNode")
      foldNode(databasePasswordNode)(
        pw => Some((databasePasswordNode).toString), b => None, l => Some(l.lexicalForm))
    } else None
  }

  /**
   * UNUSED!
   *  find User And Password from email such that:
   * ?USER foaf:mbox <email> .
   * ?USER :password ?PASSWORD .
   */
  private def findUserAndPasswordFromEmail(email: String): Option[(String, String)] = {
    val tryUserAndPassword = dataset.r {
      // query for a resource having foaf:mbox email
      val mboxTriples = find(allNamedGraph, ANY, foaf.mbox, URI(email))
      val lt = mboxTriples.toList
      if (lt.size > 0) {
        val userURI = lt(0).subject
        val pwOption = findPassword(userURI.toString)
        pwOption match {
          case Some(pw) => Some((userURI.toString, pw))
          case None => None
        }
      } else None
    }
    tryUserAndPassword match {
      case Success(v) => v
      case Failure(e) => {
        println(s"findUserAndPassword: Failure($e)")
        None
      }
    }
  }

  /**
   * record password in database; @return user Id if success
   * TODO check already existing account;
   */
  def signin(agentURI: String, password: String): Try[String] = {
    println("Authentication.signin: userId " + agentURI)
    val res = dataset.rw({
      val mGraph = passwordsGraph // .makeMGraph()
      mGraph += makeTriple(URI(agentURI), passwordPred,
        makeLiteral(password, xsd.string))
      agentURI
    })
    res
  }

  def signinOLD(agentURI: String, password: String): Try[String] = {

    // check that there is an email
    println(s"signin(agentURI=$agentURI")
    val mboxTriples =
      dataset.r({
        find(allNamedGraph, URI(agentURI), foaf.mbox, ANY)
      }).get
    val lt = mboxTriples.toList
    println("mbox Triples size " + lt.size)
    if (lt.size > 0) {
      val userId = foldNode(lt(0).objectt)(
        uri => fromUri(uri),
        bn => fromBNode(bn),
        lit => fromLiteral(lit)._1)
      println("userId " + userId)
      val r1 = dataset.rw({
        // record password in database
        val mGraph = passwordsGraph // .makeMGraph()
        mGraph += makeTriple(URI(agentURI), passwordPred,
          makeLiteral(password, xsd.string))
        Success(userId)
      })
      r1.get
    } else Failure(new Exception(
      s"""There is no email (foaf:mbox) associated to "$agentURI""""))
  }
}