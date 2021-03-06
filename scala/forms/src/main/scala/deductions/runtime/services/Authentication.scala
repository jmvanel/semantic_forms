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

  /** compare password with database; @return true if password matches login Name */
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
    println1(s">>>> findUser loginName $loginName")
    val databasePasswordOption = findPassword(loginName)
    databasePasswordOption match {
      case Some(databasePassword) => Some(loginName)
      case None => None
    }
  }

  import UserRoles._
  def findUserRole(loginName: String): UserRole = {
    ???
  }

  /** query for (digest) password in dedicated Authentication database */
  private def findPassword(userid: String): Option[(String)] = {
    /* NOTE : now we store actual URI in TDB3:
     * <user:jmvanel> <urn:password> "4124BC0A9335C27F" <urn:users> .
     * but old accounts are stored like this :( :
     * <jmvanel> <urn:password> "4124BC0A9335C27F" <urn:users> .
     * so we try both forms for compatibility with old accounts */
    val userURI = URI(makeAbsoluteURIstringForSaving(userid))
    def doFindPassword(userURI: Rdf#URI) = {
      val passwordDigestsForUser = rdfStore.r(dataset3, {
        println1(s"""findPassword: passwordsGraph:
        $passwordsGraph
        Query:
        <$userURI> <$passwordPred> ?DIGEST_FOR_PASSWORD)""")
        find(makeIGraph(passwordsGraph), userURI, passwordPred, ANY)
          .toList
      }).get
      println1(s"findPassword: passwordDigestsForUser '$userid' '$passwordDigestsForUser'")
      passwordDigestsForUser
    }
    val passwordDigestsForUser0 = doFindPassword(userURI)
    val passwordDigestsForUser = if (passwordDigestsForUser0.size > 0)
      passwordDigestsForUser0
    else
      doFindPassword(URI(userid))

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
   * record password for user URI in database; @return user Id if success;
   * store hash, not password;
   * remove existing password;
   * password and user name quality have already been checked,
   * check already existing account: only user can modify password
   * @param agentURI user ID as entered by user: URI, or else will be converted in URI part
   */
  def signin(agentURI: String, password: String): Try[String] = {
    println1(s"""Authentication.signin: userId '$agentURI'""")
    val absoluteURIstringForSaving = makeAbsoluteURIstringForSaving(agentURI)
    val absoluteURIForSaving = URI(absoluteURIstringForSaving )

    val resultStorePassword = rdfStore.rw( dataset3, {
      val mGraph = passwordsGraph
      val existingTriplesToRemove = find( makeIGraph(mGraph), absoluteURIForSaving, passwordPred, ANY ).toList
      removeTriples(mGraph, existingTriplesToRemove)
      mGraph += makeTriple(absoluteURIForSaving, passwordPred,
        makeLiteral(hashPassword(password), xsd.string))
      logger.debug(s"signin(agentURI=$agentURI) existingTriplesToRemove \n${existingTriplesToRemove.mkString("\n")}")
      absoluteURIstringForSaving
    })
    println1(s"""Authentication.signin: resultStorePassword '$resultStorePassword'""")

    // annotate the user graph URI as a foaf:OnlineAccount
    val res2OnlineAccount = rdfStore.rw(dataset, {
      val newTripleForUser = List(
          makeTriple(absoluteURIForSaving, rdf.typ, foaf.OnlineAccount),
          makeTriple(absoluteURIForSaving, rdf.typ, form("OnlineAccount"))
          )
      val newGraphForUser: Rdf#Graph = makeGraph(newTripleForUser)
      rdfStore.appendToGraph( dataset, absoluteURIForSaving, newGraphForUser)
      absoluteURIstringForSaving
    })
    println1(s"""Authentication.signin: result user graph URI as OnlineAccount : '"$res2OnlineAccount'"""")
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
