package deductions.runtime.services

import deductions.runtime.core.HTTPrequest
import org.w3.banana.RDF
import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.abstract_syntax.FormSyntaxJson
import deductions.runtime.abstract_syntax.UnfilledFormFactory

trait CreationAbstractForm [Rdf <: RDF, DATASET]
extends RDFCacheAlgo[Rdf, DATASET]
with UnfilledFormFactory[Rdf, DATASET]
with FormSyntaxJson[Rdf]{

  import ops._

  /** make raw Data for a form for instance creation; transactions Inside */
  def createData(classUri: String,
                 formSpecURI: String = "",
                 request: HTTPrequest
                 ) : FormSyntax = {
    val classURI = URI(classUri)
    retrieveURIBody(classURI, dataset, request, transactionsInside = true)
    implicit val lang = request.getLanguage
    implicit val graph: Rdf#Graph = allNamedGraph
    val form = createFormFromClass(classURI, formSpecURI, request)
    form
  }


  def createDataAsJSON(classUri: String,
                       formSpecURI: String = "",
//                       graphURI: String = "",
                       request: HTTPrequest ) = {
    val formSyntax =
//      rdfStore.rw( dataset, {
      createData(classUri, formSpecURI, request)
//    }) . get
    formSyntax2JSONString(formSyntax)
  }
}
