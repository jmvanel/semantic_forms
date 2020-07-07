/* jshint -W014 */
/* jshint -W033 */
/* jshint -W104 */

// require("jquery-ui")
// See doc here https://jqueryui.com/autocomplete/#remote

// https://github.com/dbpedia/lookup
// wget "http://lookup.dbpedia.org/api/search.asmx/PrefixSearch?QueryClass=&MaxHits=10&QueryString=berl" --header='Accept: application/json'

/* pulldown menu in <input> does show on Chrome; Opera, Android
Regular dbpedia/lookup server tried, then 2 fallbacks: lookup.dbpedia-spotlight.org, then SF server /lookup

see
https://jqueryui.com/autocomplete/
https://github.com/RubenVerborgh/dbpedia-lookup-page

TODO: translate in Scala: see
https://github.com/saig0/scala-js-jqueryui-example-app
https://index.scala-lang.org/definitelyscala/scala-js-jqueryui/scala-js-jqueryui/1.0.0?target=_sjs0.6_2.11
https://github.com/jducoeur/jquery-facade
https://github.com/sjrd/scala-js-jquery
https://github.com/scala-js/scala-js-jquery
https://www.google.fr/search?q=ajax+example+scala.js
 */

const resultsCount = 15

$(document).ready(function() {
  const lookupServer = "http://lookup.dbpedia.org"
  const alternativeLookupServer = "http://lookup.dbpedia-spotlight.org"
  function makeDbPediaLookupURL(baseURL) { return "/proxy?originalurl=" + baseURL + "/api/search/PrefixSearch"}

  const searchServiceDbPediaURL = makeDbPediaLookupURL(lookupServer)
  const lookupDbPediaCSSclass = '.hasLookup'

  const lookupLocalCSSclass = '.sfLookup'
  const searchServiceLocalURL = "/lookup"

  registerCompletionGeneric( makeAjaxDbPediaLookupProtocolFunction, lookupDbPediaCSSclass,
	  searchServiceDbPediaURL, getRDFtypeInURLastItem, prepareCompletionDbPedia )
  registerCompletionGeneric( makeAjaxDbPediaLookupProtocolFunction, lookupLocalCSSclass,
	  searchServiceLocalURL, getRDFtypeInURLfullURI, prepareCompletionLucene )

}); // end document ready function

/** prepare Completion string, DbPedia lookup syntax */
function prepareCompletionDbPedia( userString ) {
  return userString . replace( / /g, "_" )
}

/** prepare Completion string, Lucene syntax (for Jena in SF) */
function prepareCompletionLucene( userString ) {
  var stringToSearch = userString
  var words = stringToSearch .split(' ')
  if( words . length > 1 )
    stringToSearch = encodeURIComponent( words[0] + '+AND+' +  words[1] )
  return stringToSearch
}

const makeAjaxDbPediaLookupProtocolFunction =
/** @param request: user input for completion */
function(searchServiceURL, request, inputElement, callback, getRDFtypeInURL,
         prepareCompletionString){
  console.log("Trigger HTTP on <" + searchServiceURL + "> for '" + request.term + "'")
  var stringToSearch = prepareCompletionString(request.term)
  return (
    $.ajax({
        url: searchServiceURL + "?QueryString=" + stringToSearch
                              + "&MaxHits="+resultsCount
                              + encodeURIComponent(getRDFtypeInURL(inputElement)) ,
        dataType: "json" ,
        timeout: 30000
    }).done( function (ajaxResponse) {
        console.log('Ajax Response from ' + searchServiceURL)
        console.log(ajaxResponse)
        callback( prettyPrintURIsFromDbpediaResponse(ajaxResponse) )
    })
  )
}

function prettyPrintURIsFromDbpediaResponse(ajaxResponse){
  return ajaxResponse.results.map(
    function (m) {
      return {
	      "label": m.label + " - "
	        + cutStringAfterCharacter(m.description, '.')
                +  " - " + m.classes
                +  " - refCount " + m.refCount ,
	      "value": m.uri }
  })
}

