package deductions.runtime.services

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import org.w3.banana.FOAFPrefix
import org.w3.banana.RDF

import deductions.runtime.sparql_cache.RDFCacheAlgo

/**
 * @author jmv
 */
trait Authentication[Rdf <: RDF, DATASET] extends RDFCacheAlgo[Rdf, DATASET] {

  val passwordsGraph: Rdf#Graph

  import ops._
  val foaf = FOAFPrefix[Rdf]
  val passwordPred = URI("urn:password")

  /** compare password with database; @return user URI if success */
  def login(loginName: String, password: String): Option[Rdf#URI] = {

    // query for a resource having foaf:mbox loginName
    val mboxTriples = find(allNamedGraph, ANY, foaf.mbox, URI(loginName))
    val lt = mboxTriples.toList
    if (lt.size > 0) {
      val userURI = lt(0).subject

      // query for password in dedicated database
      val pwds = find(passwordsGraph, userURI, passwordPred, ANY)
      val pwdsl = pwds.toList

      // compare password
      if (pwds.size > 0) {
        val databasePassword = pwdsl(0).subject
        if (databasePassword == password) {
          foldNode(userURI)(
            u => Some(u), b => None, l => None)
        } else None
      } else None
    } else None
  }

  /** record password in database; @return user Id (email address) if success */
  def signin(agentURI: String, password: String): Try[String] = {

    // check that there is an email
    val mboxTriples = find(allNamedGraph, URI(agentURI), foaf.mbox, ANY)
    val lt = mboxTriples.toList
    if (lt.size > 0) {
      val userId = foldNode(lt(0).objectt)(
        uri => fromUri(uri),
        bn => fromBNode(bn),
        lit => fromLiteral(lit)._1)
      // record password in database
      val mGraph = passwordsGraph.makeMGraph()
      mGraph += makeTriple(URI(agentURI), passwordPred,
        makeLiteral(password, xsd.string))
      Success(userId)
    } else Failure(new Exception(
      s"""There is no email (foaf:mbox) associated to "$agentURI""""))
  }
}