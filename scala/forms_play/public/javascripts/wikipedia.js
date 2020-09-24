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
  //function makeDbPediaLookupURL(baseURL) { return "/proxy?originalurl=" + baseURL + "/api/search/PrefixSearch"}
  function makeDbPediaLookupURL(baseURL) {
	// return makeProxiedLookupURLifNecessary( baseURL + "/api/search") }
	return makeProxiedLookupURLifNecessary( baseURL + "/api/search/PrefixSearch") }

  const searchServiceDbPediaURL = makeDbPediaLookupURL(lookupServer)
  const lookupDbPediaCSSclass = '.hasLookup'

  const lookupLocalCSSclass = '.sfLookup'
  const searchServiceLocalURL = "/lookup"

  // register Completion for DbPedia Lookup
  registerCompletionGeneric( makeAjaxDbPediaLookupProtocolFunction, lookupDbPediaCSSclass,
	  searchServiceDbPediaURL, getRDFtypeInURLastItem, prepareCompletionDbPedia )

  // register Completion for Local Lookup, same protocol as DbPedia, but different search string preparation
  registerCompletionGeneric( makeAjaxDbPediaLookupProtocolFunction, lookupLocalCSSclass,
	  searchServiceLocalURL, getRDFtypeInURLfullURI, prepareCompletionLucene )

}); // end document ready function

/** prepare Completion string, DbPedia lookup syntax */
function prepareCompletionDbPedia( userString ) {
  return userString . replace( / /g, "_" )
}

const makeAjaxDbPediaLookupProtocolFunction_NEW =
/** @param request: user input for completion
 * see new API https://github.com/dbpedia/dbpedia-lookup#using-the-lookup-service
 * @return ajax jquery object to used as source for autocomplete in registerCompletionGeneric()
 * */
function(searchServiceURL, request, inputElement, callback, getRDFtypeInURL,
         prepareCompletionString){
  console.log("Trigger HTTP search Service <" + searchServiceURL + "> for '" + request.term + "'")
  var stringToSearch = prepareCompletionString(request.term)
  var QueryClass = getRDFtypeInURL(inputElement)
  var QueryClassParam = "&typeName=" + QueryClass
  if(QueryClass == "") QueryClassParam = ""
  var urlComplete = searchServiceURL + encodeURIComponent(
		"?query=" + stringToSearch
                              + "&maxResults=" + resultsCount
                              + "&format=json"
                              + QueryClassParam )
  console.log("HTTP URL <" + urlComplete + ">" )
  return (
    $.ajax({
        url: urlComplete ,
        dataType: "json" ,
        timeout: 30000
    }).done( function (ajaxResponse) {
        console.log('Ajax Response from ' + searchServiceURL)
        console.log(ajaxResponse)
        callback( prettyPrintURIsFromDbpediaResponse(ajaxResponse) )
    })
  )
}

const makeAjaxDbPediaLookupProtocolFunction =
/** @param request: user input for completion
 * @return ajax jquery object to used as source for autocomplete in registerCompletionGeneric()
 * */
function(searchServiceURL, request, inputElement, callback, getRDFtypeInURL,
         prepareCompletionString){
  console.log("Trigger HTTP on <" + searchServiceURL + "> for '" + request.term + "'")
  var stringToSearch = prepareCompletionString(request.term)
  console.log("1 " + stringToSearch + "'")
  var QueryClass = getRDFtypeInURL(inputElement)
  console.log("2 QueryClass '" + QueryClass + "'")
  var QueryClassParam = "&QueryClass=" + QueryClass
  console.log("3 " + QueryClassParam + "'")
  if(QueryClass == "") QueryClassParam = ""
  console.log("4 QueryClassParam '" + QueryClassParam + "'")

  var httpParamsRaw = "?QueryString=" + stringToSearch
                              + "&MaxHits="+resultsCount
                              + "&format=json"
                              + QueryClassParam
  var httpParams = httpParamsRaw
  var isExternalServer =
    searchServiceURL . startsWith( "http" ) ||
    searchServiceURL . startsWith( "/proxy" )

  if( isExternalServer )
    httpParams = encodeURIComponent( httpParamsRaw )

  var urlComplete = searchServiceURL + httpParams
  console.log("HTTP URL <" + urlComplete + ">" )

  return (
    $.ajax({
        url: urlComplete,
        dataType: "json" ,
        timeout: 30000
    }).done( function (ajaxResponse) {
        console.log('Ajax Response from ' + searchServiceURL)
        console.log(ajaxResponse)
        if( isExternalServer )
          callback( prettyPrintURIsFromDbpediaResponse(ajaxResponse) )
	else
          callback( prettyPrintURIsFromDbpediaResponse_Local(ajaxResponse) )
    })
    // NOTE : fail() is taken care in lookup-generic.js
  )
}

function prettyPrintURIsFromDbpediaResponse(ajaxResponse){
  return ajaxResponse.docs.map(
    function (m) {
      return {
	      "label":
	        // "<div>"
	        m.label[0] + " - "
	        + ( m["comment"] === undefined ? "" : cutStringAfterCharacter(m.comment[0], '.') )
                + ( m["typeName"] === undefined ? "" : " - " + m.typeName .join(", ") )
                + ( m["refCount"] === undefined ? "" : " - refCount " + m.refCount[0] )
	        + " - <" + m.resource[0] + ">"
	        //  + "</div>"
	      ,
	      "value": m.resource[0] }
  })
}

/** for local /lookup server; uses old JSON structure */
function prettyPrintURIsFromDbpediaResponse_Local(ajaxResponse){
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

