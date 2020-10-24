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
  const lookupServer = "https://taxref.mnhn.fr/sparql"
    // "https://taxref.i3s.unice.fr/sparql" // avoid use this (Franck Michel)
    // "http://sparks-vm33.i3s.unice.fr:8890/sparql"

  const lookupCSSclass = '.virtuosoLookup'
  const suggestionSearchCSSclass = 'sf-suggestion-search-dbpedia'

  registerCompletionGeneric( makeAjaxSPARQLlookupProtocolFunction, lookupCSSclass ,
                             lookupServer, getRDFtypeInURLfullURI, prepareCompletionVirtuoso )

}); // end document ready function

/** prepare Completion string, Virtuoso syntax */
function prepareCompletionVirtuoso( userString ) {
  var stringToSearch = userString.trim()
  var words = stringToSearch .split(' ')
  if( words . length > 1 && words[1].length >= 4)
    stringToSearch = `"${words[0]}*" AND "${words[1]}*"`
  else
    stringToSearch = `"${words[0]}*"`
  console.log( `stringToSearch '${stringToSearch}'`)
  return stringToSearch
}

function getUserLanguage() {
  var userLang = navigator.language || navigator.userLanguage; 
  return userLang.replace(RegExp("-.*"), "")
}

const makeAjaxSPARQLlookupProtocolFunction =
/** @param request: user input for completion */
function(searchServiceURL, request, inputElement, callback, getRDFtypeInURL,
         prepareCompletionString){
  console.log("Trigger HTTP on <" + searchServiceURL + "> for '" + request.term + "'")
  var stringToSearch = prepareCompletionString(request.term)
  var USER_LANG = getUserLanguage()
/* 	    # ?s1 <http://taxref.mnhn.fr/lod/property/vernacularName> ?vernac .
	    # ?vernac bif:contains ' ( ${stringToSearch} ) '
	    # option ( score ?sc2 ) . */
  return (
    $.ajax({
      url: makeProxiedLookupURLifNecessary(
	    searchServiceURL +
	   "/sparql?default-graph-uri=&query=" +
	    encodeURIComponent(
     `PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
      select DISTINCT ?s1 as ?c1, ( bif:search_excerpt ( bif:vector () , ?o1 ) ) as ?c2, ?sc, ?rank, ?LAB, ?VERN where {
        select ?s1, ( ?sc * 3e-1 ) as ?sc, ?o1, ( sql:rnk_scale ( <LONG::IRI_RANK> ( ?s1 ) ) ) as ?rank, ?LAB, ?VERN where {
            graph ?g {
              ?s1 ?s1textp ?o1 .
              ?o1 bif:contains ' ( ${stringToSearch} ) ' option ( score ?sc ) .
            }
            graph <http://taxref.mnhn.fr/lod/graph/vernacular/13.0> {
			# OPTIONAL { 
              ?s1 <http://taxref.mnhn.fr/lod/property/vernacularName> ?VERN . # }
	      # FILTER( LANG(?VERN) = "${USER_LANG}" )
            }
            graph <http://taxref.mnhn.fr/lod/graph/classes/13.0> {
              ?s1 rdfs:label ?LAB .
              ?s1 a owl:Class .
            }
         }
       }
       order by desc ( ?sc * 3e-1 + sql:rnk_scale ( <LONG::IRI_RANK> ( ?s1 ) ) ) limit 40 offset 0
   ` // end Template literal
   ) // end encodeURIComponent
      ) // end makeProxiedLookupURL
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
	      "label": m.LAB.value + " - "
                +  " - " + m.VERN.value
                +  " - rank " + m.rank.value
                +  " - <" + m.c1.value + ">",
	      "value": m.c1.value }
  })
}

