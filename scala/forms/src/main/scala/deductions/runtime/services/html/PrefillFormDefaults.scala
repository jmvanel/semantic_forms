package deductions.runtime.services.html

import deductions.runtime.abstract_syntax.FormSyntaxJson
import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.utils.RDFTreeDuplicator
import deductions.runtime.core.HTTPrequest
import deductions.runtime.user.UserQueries

import org.w3.banana.RDF

import java.net.URLDecoder
import java.time.format.DateTimeFormatter
import java.time.LocalDate

trait PrefillFormDefaults[Rdf <: RDF, DATASET]
extends RDFCacheAlgo[Rdf, DATASET]
with RDFPrefixes[Rdf]
with FormSyntaxJson[Rdf]
with UserQueries[Rdf, DATASET] {
  import ops._

  /** prefill defaults: user FOAF profile, date */
  def prefillFormDefaults(form: FormSyntax, request: HTTPrequest) : FormSyntax = {
    import request._
    def localDateText = {
      val iso8601DateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
      val date = LocalDate.now();
      date.format(iso8601DateFormatter)
    }
    val newFfields = for (field <- form.fields) yield {
      field match {
        case field: LiteralEntry if (field.property == dct("date") ) =>
          field.copy(value = Literal(localDateText))
        case field: ResourceEntry if (field.property == foaf("maker") ) =>
          field.copy( value = getPersonFromAccountTR(request.userId()) )
        case _ => field
      }
    }        
    form.fields = newFfields
    form
  }

}