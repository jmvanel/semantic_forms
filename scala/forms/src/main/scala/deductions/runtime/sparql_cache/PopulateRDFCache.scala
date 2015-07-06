package deductions.runtime.sparql_cache

import org.apache.jena.riot.RDFDataMgr
import org.w3.banana.RDFOpsModule
import org.w3.banana.jena.JenaModule
import org.w3.banana.RDFStore
import org.w3.banana.GraphStore
import deductions.runtime.jena.RDFStoreObject
import org.w3.banana.Prefix
import org.w3.banana.SparqlUpdate
import org.w3.banana.RDFOps
import org.w3.banana.RDFDSL
import scala.util.Failure
import scala.util.Try
import scala.util.Success
import org.w3.banana.LocalNameException
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.jena.JenaHelpers
import deductions.runtime.jena.RDFCache
import org.w3.banana.jena.Jena
import com.hp.hpl.jena.query.Dataset
import deductions.runtime.dataset.RDFStoreLocalProvider
import org.w3.banana.RDF

trait SitesURLForDownload {
  val githubcontent: String = "https://raw.githubusercontent.com"
}

/**
 * Populate RDF Cache with commonly used vocabularies;
 *  should be done just once;
 *
 *  same as loading
 *  http://svn.code.sf.net/p/eulergui/code/trunk/eulergui/examples/defaultVocabularies.n3p.n3
 *  but without any dependency to EulerGUI.
 *
 *  TODO use prefix.cc web service to load from prefix short names (see implementation in EulerGUI)
 *
 *  TODO move in jena package ( and update README )
 */
object PopulateRDFCache extends JenaModule
  with JenaHelpers
  with RDFStoreLocalJena1Provider
  with PopulateRDFCacheTrait[Jena, Dataset]

trait PopulateRDFCacheTrait[Rdf <: RDF, DATASET]
    extends RDFOpsModule
    with CommonVocabulariesLoaderTrait[Rdf, DATASET]
    with RDFI18NLoaderTrait[Rdf, DATASET]
    with FormSpecificationsLoaderTrait[Rdf, DATASET]
    //    with RDFStoreLocalProvider[Rdf, DATASET]
    //    with JenaHelpers
    with App {

  import ops._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._

  loadCommonVocabularies()
  loadCommonFormSpecifications()
  loadFromGitHubRDFI18NTranslations()
}
