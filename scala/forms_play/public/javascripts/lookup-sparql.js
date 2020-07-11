/* jshint -W014 */
/* jshint -W033 */
/* jshint -W104 */

// require("jquery-ui")
// See doc here https://jqueryui.com/autocomplete/#remote

/** see
https://jqueryui.com/autocomplete/

TODO: translate in Scala: see
https://github.com/saig0/scala-js-jqueryui-example-app
https://index.scala-lang.org/definitelyscala/scala-js-jqueryui/scala-js-jqueryui/1.0.0?target=_sjs0.6_2.11
https://github.com/jducoeur/jquery-facade
https://github.com/sjrd/scala-js-jquery
https://github.com/scala-js/scala-js-jquery
https://www.google.fr/search?q=ajax+example+scala.js
*/


$(document).ready(function() {
  const resultsCount = 15
  const lookupServer = "https://taxref.i3s.unice.fr"
		// "http://sparks-vm33.i3s.unice.fr:8890/sparql"

  const lookupCSSclass = '.virtuosoLookup'
  const suggestionSearchCSSclass = 'sf-suggestion-search-dbpedia'

  registerCompletionGeneric( makeAjaxSPARQLlookupProtocolFunction, lookupCSSclass ,
                             lookupServer, getRDFtypeInURLfullURI )

}); // end document ready function

// function prepareCompletionVirtuoso( userString ) { return userString .  }

const makeAjaxSPARQLlookupProtocolFunction =
/** @param request: user input for completion */
function(searchServiceURL, request, inputElement, callback, getRDFtypeInURL){
  console.log("Trigger HTTP on <" + searchServiceURL + "> for '" + request.term + "'")
  var stringToSearch = request.term
  var words = stringToSearch .split(' ')
  if( words . length > 1 )
    stringToSearch = words[0] + '*_' +  words[1] + '*'
  else
    stringToSearch = `"${stringToSearch}*"`
  console.log( "stringToSearch '" + stringToSearch + "'" )

  return (
    $.ajax({
      url: searchServiceURL +
	   "/sparql?default-graph-uri=&query=" +
	    encodeURIComponent(
	    `select ?s1 as ?c1, ( bif:search_excerpt ( bif:vector () , ?o1 ) ) as ?c2, ?sc, ?rank, ?g where {
        select ?s1, ( ?sc * 3e-1 ) as ?sc, ?o1, ( sql:rnk_scale ( <LONG::IRI_RANK> ( ?s1 ) ) ) as ?rank, ?g where
        {
          quad map virtrdf:DefaultQuadMap
          {
            graph <http://taxref.mnhn.fr/lod/graph/classes/13.0>
            {
              ?s1 <http://www.w3.org/2000/01/rdf-schema#label> ?o1 .
              ?o1 bif:contains ' ( ${stringToSearch} ) '
		    option ( score ?sc ) .
	    # ?s1 <http://taxref.mnhn.fr/lod/property/vernacularName> ?vernac .
	    # ?vernac bif:contains ' ( ${stringToSearch} ) '
	    # option ( score ?sc2 ) .
            }
           }
         }
       order by desc ( ?sc * 3e-1 + sql:rnk_scale ( <LONG::IRI_RANK> ( ?s1 ) ) ) limit 20 offset 0
   }` // end Template literal
   ) // end encodeURIComponent
    // + "&format=application/sparql-results+json&timeout=0" // should work , but Virtuoso minor bug!
    + "&format=application/sparql-results%2Bjson&timeout=0"
	,
        dataType: "json" ,
        timeout: 30000
    }).done( function (ajaxResponse) {
        console.log('Ajax Response from ' + searchServiceURL)
        console.log(ajaxResponse)
        callback( prettyPrintURIsFromSPARQLresponse(ajaxResponse) )
    })
  )
}

/** adapted to Virtuoso e.g.
 * http://sparks-vm33.i3s.unice.fr:8890/fct/facet.vsp?cmd=set_text_property&iri=http%3A%2F%2Fwww.w3.org%2F2000%2F01%2Frdf-schema%23label&lang=&datatype=uri&sid=274 */
function prettyPrintURIsFromSPARQLresponse(ajaxResponse){
  return ajaxResponse.results.bindings.map(
    function (m) {
      return {
	      "label": m.c2.value + " - "
                +  " - rank " + m.rank.value ,
	      "value": m.c1.value }
  })
}

