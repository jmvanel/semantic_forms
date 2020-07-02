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

const searchServiceURL = makeDbPediaLookupURL(lookupServer)
const lookupCompletionCSSclass = '.hasLookup'
const suggestionSearchCSSclass = 'sf-suggestion-search-dbpedia'


$(".sf-standard-form").on( 'focus', lookupCompletionCSSclass, function(event) {
	var inputElement = $(this);
        $(this).autocomplete({
            autoFocus: true,
            minlength: 3,
            search: function() {
                $(this).addClass(suggestionSearchCSSclass)
            },
            open: function() {
                $(this).removeClass(suggestionSearchCSSclass)
            },
            select: function( event, ui ) {
                console.log( "Topic chosen label event ");
                console.log($(this));
                console.log( "Topic chosen label ui");
                console.log(ui);
                $emptyFields = $(this).siblings().filter(function(index) { return $(this).val() == ''}).length;
                console.log('Champs vides : '+ $emptyFields);
                if ($emptyFields === 0) {
                    addedWidget = cloneWidget($(this))
                }
            },
            source: function(request, callback) {
              console.log(" source: function: request .term '" + request .term + "'");
              var inputElementContainsURL =
			request .term. startsWith("http://") ||
			request .term. startsWith("urn:")

              if( inputElementContainsURL )
			inputElement.removeClass(suggestionSearchCSSclass);
              else {

                var ajax = makeAjaxDbPediaLookupProtocol( searchServiceURL, request, inputElement, callback)
                console.log(ajax)

                ajax.fail(function (error){

                  var ajax = makeAjaxDbPediaLookupProtocol( makeDbPediaLookupURL(alternativeLookupServer), request, inputElement, callback)
                  console.log(ajax)
                  ajax.fail(function (error){

                   // in lookup.js, the same completion is launched on CSS class .sfLookup
                   if( ! inputElement.hasClass('.sfLookup') ) {
                    console.log("lookup.dbpedia.org FAILED: error:" + error.statusText )
                    console.log(error )
                    console.log("lookup.dbpedia.org FAILED => launch local /lookup '" + request.term + "'" )
                    var ajax = makeAjaxDbPediaLookupProtocol( "/lookup", request, inputElement, callback)
                    console.log(ajax)
                   }
                  })
                })
            }
            }
        })
    });
});

/** @param request: user input for completion */
function makeAjaxDbPediaLookupProtocol(searchServiceURL, request, inputElement, callback){
  console.log("Trigger HTTP on <" + searchServiceURL + "> for '" + request.term + "'")
  return (
    $.ajax({
        url: searchServiceURL + "?QueryString=" + request.term . replace( / /g, "_" )
                              + "&MaxHits="+resultsCount
                              + getRDFtypeInURL(inputElement) ,
        dataType: "json" ,
        timeout: 30000
    }).done( function (ajaxResponse) {
        console.log('Ajax Response from ' + searchServiceURL)
        console.log(ajaxResponse)
        callback( prettyPrintURIsFromDbpediaResponse(ajaxResponse) )
    })
  )
}

function cutStringAfterCharacter(s, c) {
    if (s !== null) {
        var n = s.indexOf(c);
        return s.substring(0, n != -1 ? n : s.length);
    } else {
        return "_"
    }
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

/** 		// DONE added QueryClass
		// compare results: QueryClass=person , and ?QueryClass=place
		// view-source:http://lookup.dbpedia.org/api/search/PrefixSearch?QueryClass=Person&QueryString=berlin
		// view-source:http://lookup.dbpedia.org/api/search/PrefixSearch?QueryClass=Place&QueryString=berlin

*/
function getRDFtypeInURL(inputElement) {
		// QueryClass comes from attribute data-rdf-type in <input> tag , but data-rdf-type is a full URI !
                var typeName = "";
                // var $el = $(event.target);
                var $el = inputElement;
                if( $el && $el.attr('data-rdf-type') ) {
                   type = $el.attr('data-rdf-type').split('/');
                    if (type) {
                      typeName = type[type.length - 1];
                      console.log('typeName ' + typeName)
                    }
                }
                if( typeName.length > 0 )
                  typeNameInURL = "&QueryClass="+typeName
                else
                  typeNameInURL = ""
  return typeNameInURL
}
