package deductions.runtime.services

import java.security.MessageDigest
import javax.xml.bind.annotation.adapters.HexBinaryAdapter

import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.utils.RDFStoreLocalUserManagement
import deductions.runtime.utils.{RDFPrefixes, URIManagement}
import org.w3.banana.RDF

import scala.util.Try

import scalaz._
import Scalaz._


/** facade for user Authentication management;
 *  wraps the TDB3 database
 * @author jmv
 */
trait Authentication[Rdf <: RDF, DATASET] extends RDFCacheAlgo[Rdf, DATASET]
    with RDFPrefixes[Rdf]
    with RDFStoreLocalUserManagement[Rdf, DATASET]
    with URIManagement {

  def passwordsGraph: Rdf#MGraph


  import ops._

  val passwordPred = URI("urn:password")
  val userRolePred = URI("urn:userRole")

  /** compare password with database; @return user URI if success */
  def checkLogin(loginName: String, password: String): Boolean = {
    val databasePasswordOption = findPassword(loginName)
    databasePasswordOption match {
      case Some(databasePassword) => databasePassword === hashPassword(password)
      case None => false
    }
  }

  /**
   * find User in registered logins database.
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

  import UserRolesModes._
  import UserRoles._
  def findUserRole(loginName: String): UserRole = {
    ???
  }

  /** query for (digest) password in dedicated Authentication database */
  private def findPassword(userid: String): Option[(String)] = {
    val userURI = URI(userid)

    val passwordDigestsForUser = rdfStore.r( dataset3, {
      println1( s"""findPassword: passwordsGraph:
        $passwordsGraph
        Query:
        <$userURI> <$passwordPred> ?DIGEST_FOR_PASSWORD)""" )
      find( makeIGraph(passwordsGraph), userURI, passwordPred, ANY)
      .toList
    }).get

    println1( s"findPassword: passwordDigestsForUser $passwordDigestsForUser" )

    if (passwordDigestsForUser.size > 0) {
      val databasePasswordNode = passwordDigestsForUser(0).objectt
      println1(s"findPassword: findUserAndPassword $databasePasswordNode")
      foldNode(databasePasswordNode)(
        pw => Some((databasePasswordNode).toString),
        b => None,
        l => Some(l.lexicalForm))
    } else None
  }

  /**
   * UNUSED!
   *  find User And Password from email such that:
   * ?USER foaf:mbox <email> .
   * ?USER :password ?PASSWORD .
   */
//  private def findUserAndPasswordFromEmail(email: String): Option[(String, String)] = {
//    val tryUserAndPassword = dataset3.r {
//      // query for a resource having foaf:mbox email
//      val mboxTriples = find(allNamedGraph, ANY, foaf.mbox, URI(email))
//      val lt = mboxTriples.toList
//      if (lt.size > 0) {
//        val userURI = lt(0).subject
//        val pwOption = findPassword(userURI.toString)
//        pwOption match {
//          case Some(pw) => Some((userURI.toString, pw))
//          case None => None
//        }
//      } else None
//    }
//    tryUserAndPassword match {
//      case Success(v) => v
//      case Failure(e) => {
//        println1(s"findUserAndPassword: Failure($e)")
//        None
//      }
//    }
//  }

  def listUsers(): List[Rdf#Node] = {
    val pwds = rdfStore.r(dataset3, {
      println1(s"""listUsers: find( makeIGraph(
        $passwordsGraph) , passwordPred, ANY)""")
      find(makeIGraph(passwordsGraph), ANY, passwordPred, ANY)
    }).get

    pwds.map { tr => tr.subject }.toList
  }

  /**
   * record password in database; @return user Id if success
   * store hash , not password
   * TODO check already existing account;
   */
  def signin(agentURI: String, password: String): Try[String] = {
    println1(s"""Authentication.signin: userId "$agentURI"""")

    // TODO ? probably use absoluteURIForSaving instead of userUri everywhere
    val userUri = agentURI //  makeURIPartFromString(agentURI)
    val absoluteURIForSaving = URI(makeAbsoluteURIForSaving(userUri))

    /* NOTE: here we are putting password triple in named graph <userUri>,
     * which is generally a NON absolute URI;
     * but the passwords graph and database is probably only accessed by API (never by SPARQL),
     * so this is not a problem */
    val resultStorePassword = rdfStore.rw( dataset3, {
      val mGraph = passwordsGraph
      mGraph += makeTriple(URI(userUri), passwordPred,
        makeLiteral(hashPassword(password), xsd.string))
      userUri
    })
    println1(s"""Authentication.signin: resultStorePassword "$resultStorePassword"""")

    // annotate the user graph URI as a foaf:OnlineAccount
    val res2OnlineAccount = rdfStore.rw(dataset, {
      val newTripleForUser = List(makeTriple(absoluteURIForSaving, rdf.typ, foaf.OnlineAccount))
      val newGraphForUser: Rdf#Graph = makeGraph(newTripleForUser)
      rdfStore.appendToGraph( dataset, absoluteURIForSaving, newGraphForUser)
      userUri
    })
    println1(s"""Authentication.signin: resultStorePassword "$res2OnlineAccount"""")
    resultStorePassword
  }

//  private def signinOLD(agentURI: String, password: String): Try[String] = {
//    println1("Authentication.signin: userId " + agentURI)
//    val res = rdfStore.rw( dataset3, {
//      val mGraph = passwordsGraph
//      mGraph += makeTriple(URI(agentURI), passwordPred,
//        makeLiteral(hashPassword(password), xsd.string))
//      agentURI
//    })
//    res
//  }

  /** MD5 digest for given string */
  def hashPassword(password: String): String = {
    new HexBinaryAdapter().marshal(MessageDigest.getInstance("MD5").
        digest(password.getBytes))
  }

  def println1(message: String) =
//      println(message)
    logger.debug(message)

}
