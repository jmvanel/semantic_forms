package deductions.runtime.sparql_cache.algos

import scala.util.Success

import org.w3.banana.RDF

import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.dataset.RDFStoreLocalUserManagement
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.Authentication
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.services.SPARQLHelpers
import deductions.runtime.services.URIManagement
import deductions.runtime.utils.RDFHelpers

trait UserDataRecovery[Rdf <: RDF, DATASET]
    extends SPARQLHelpers[Rdf, DATASET]
    with Authentication[Rdf, DATASET]
    with RDFHelpers[Rdf]
    with URIManagement {

  import ops._

  /** */
  def recoverUserData = {
    val users = listUsers()
    println(s"recoverUserData: users count ${users.size}")
    for (user <- users) yield {
      println(s"recoverUserData: user <$user>")
      foldNode(user)(
        uri => {
          if ( !isAbsoluteURI(fromUri(uri)) && !fromUri(uri).startsWith("user:")) {
        	  val userGraphURI = URI( makeAbsoluteURIForSaving( makeURIFromString(fromUri(uri)) ) )
        	  println(s"recoverUserData: recover user <$user> => <$userGraphURI>")
//            val userGraphURI = URI("user:" + fromUri(uri))
            val userGraphContent = rdfStore.getGraph(dataset, uri).get
            println(s"""recoverUserData: userGraphContent size ${userGraphContent.size}
            ${userGraphContent.triples.mkString("\n\t")}""")
            rdfStore.appendToGraph(dataset, userGraphURI, userGraphContent)
            rdfStore.removeTriples(dataset, uri, userGraphContent.triples)
          } else Success(())
        },
        bn => Success(()),
        lit => Success(()))
    }
  }
}

object UserDataRecoveryApp extends ImplementationSettings.RDFCache
    with RDFStoreLocalProvider[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with RDFStoreLocalUserManagement[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with UserDataRecovery[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with App
{
  val config = new DefaultConfiguration {
    override val needLoginForEditing = true
    override val needLoginForDisplaying = true
    override val useTextQuery = false
  }
  recoverUserData
}
  
