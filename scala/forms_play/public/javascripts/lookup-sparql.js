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
  const lookupServer = "http://sparks-vm33.i3s.unice.fr:8890/sparql" // used for printing log

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
    stringToSearch = stringToSearch + "*"
  console.log( "stringToSearch '" + stringToSearch + "'" )
  return (
    $.ajax({
      url: makeProxiedLookupURL(
	    "http://sparks-vm33.i3s.unice.fr:8890/sparql?default-graph-uri=&query=+select+%3Fs1+as+%3Fc1%2C+%28+bif%3Asearch_excerpt+%28+bif%3Avector+%28+%27TINCTORIA%27%2C+%27GENISTA%27+%29+%2C+%3Fo1+%29+%29+as+%3Fc2%2C+%3Fsc%2C+%3Frank%2C+%3Fg+where+%0D%0A++%7B+%0D%0A++++%7B+%0D%0A++++++%7B+%0D%0A++++++++select+%3Fs1%2C+%28+%3Fsc+*+3e-1+%29+as+%3Fsc%2C+%3Fo1%2C+%28+sql%3Arnk_scale+%28+%3CLONG%3A%3AIRI_RANK%3E+%28+%3Fs1+%29+%29+%29+as+%3Frank%2C+%3Fg+where+%0D%0A++++++++%7B+%0D%0A++++++++++quad+map+virtrdf%3ADefaultQuadMap+%0D%0A++++++++++%7B+%0D%0A++++++++++++graph+%3Chttp%3A%2F%2Ftaxref.mnhn.fr%2Flod%2Fgraph%2Fclasses%2F13.0%3E+%0D%0A++++++++++++%7B+%0D%0A++++++++++++++%3Fs1+%3Chttp%3A%2F%2Fwww.w3.org%2F2000%2F01%2Frdf-schema%23label%3E+%3Fo1+.%0D%0A++++++++++++++%3Fo1+bif%3Acontains+%27+%28+" +
    '%22' + stringToSearch + '%22' +// " TINCTORIA+AND+GENISTA "
    "+%29+%27+option+%28+score+%3Fsc+%29+.%0D%0A++++++++++++++%0D%0A++++++++++++%7D%0D%0A+++++++++++%7D%0D%0A+++++++++%7D%0D%0A+++++++order+by+desc+%28+%3Fsc+*+3e-1+%2B+sql%3Arnk_scale+%28+%3CLONG%3A%3AIRI_RANK%3E+%28+%3Fs1+%29+%29+%29+limit+20+offset+0+%0D%0A++++++%7D%0D%0A+++++%7D%0D%0A+++%7D%0D%0A++&format=application%2Fsparql-results%2Bjson&timeout=0" ) ,
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

