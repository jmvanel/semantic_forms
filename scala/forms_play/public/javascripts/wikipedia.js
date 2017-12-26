// require("jquery-ui")

// https://github.com/dbpedia/lookup
// wget "http://    lookup.dbpedia.org/api/search.asmx/PrefixSearch?QueryClass=&MaxHits=10&QueryString=berl" --header='Accept: application/json'

/* pulldown menu in <input> does show on Chrome; Opera, Android ????
see
https://jqueryui.com/autocomplete/
https://github.com/RubenVerborgh/dbpedia-lookup-page
alternate implementation, abandoned: http://blog.teamtreehouse.com/creating-autocomplete-dropdowns-datalist-element
TODO: translate in Scala:
https://github.com/scala-js/scala-js-jquery
https://www.google.fr/search?q=ajax+example+scala.js
 */

$(document).ready(function() {

    var resultsCount = 15;
    var urlReqPrefix = "http://lookup.dbpedia.org/api/search.asmx/PrefixSearch?QueryClass=&MaxHits=" +
      resultsCount + "&QueryString=" ;
    var suggestionSearchCSSclass = 'sf-suggestion-search-dbpedia';
    var topics = [];

    $(".sf-standard-form").on('focus', '.hasLookup', function(event) {
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
		console.log(" source: function: request .term " + request .term);
		var inputElementContainsURL =
			request .term. startsWith("http://") ||
			request .term. startsWith("urn:") ;
		if( inputElementContainsURL )
			// $(this).removeClass(suggestionSearchCSSclass);
			inputElement.removeClass(suggestionSearchCSSclass);
		// if( ! request .term. startsWith("http://") )
		else {
                console.log("Déclenche l'événement lookup.dbpedia.org pour " + request.term )

		// DONE added QueryClass
		// compare results: QueryClass=person , and ?QueryClass=place
		// view-source:http://lookup.dbpedia.org/api/search/PrefixSearch?QueryClass=Person&QueryString=berlin
		// view-source:http://lookup.dbpedia.org/api/search/PrefixSearch?QueryClass=Place&QueryString=berlin

		// QueryClass comes from attribute data-rdf-type in <input> tag , but data-rdf-type is a full URI !
                var typeName = "";
                var $el = $(event.target);
                if ($el) {
                if( $el.attr('data-rdf-type') ) {
                   type = $el.attr('data-rdf-type').split('/');
                    if (type) {
                      typeName = type[type.length - 1];
                      console.log('typeName ' + typeName)
                    }
                }
                }

                $.ajax({
                    url: "http://lookup.dbpedia.org/api/search/PrefixSearch",
                    data: { MaxHits: resultsCount, QueryClass: typeName, QueryString: request.term },
//                  data: { MaxHits: resultsCount, QueryString: request.term },
                    dataType: "json"
                    , timeout: 30000
                }).done(function (response) {
                    console.log(response)
                    callback(response.results.map(function (m) {
                        // topics[m.label] = m.uri;
                        return { "label": m.label + " - " +
                        cutStringAfterCharacter(m.description, '.')
			+  " - " + m.classes
			+  " - refCount " + m.refCount
			, "value": m.uri }
                    }));

                }).fail(function (error){

                    // in lookup.js, the same completion is launched on CSS class .sfLookup
                    if( ! $el.hasClass('.sfLookup') ) {

                      console.log("lookup.dbpedia.org FAILED: error:" + error.statusText )
                      console.log(error )
                      console.log("lookup.dbpedia.org FAILED => launch local /lookup " + request.term )
                    /* TODO:
                     * - in secure context (window.isSecureContext == true) /lookup is launched with http,
                     *   which entails message:
                         jquery.min.js:4 Mixed Content: The page at 'https://semantic-forms.cc:5555/create?uri=bioc%3APlanting&uri=http%3A%2F%2F….com%2Fjmvanel%2Fsemantic_forms%2Fmaster%2Fvocabulary%2Fforms%23personForm' was loaded over HTTPS, but requested an insecure XMLHttpRequest endpoint 'http://lookup.dbpedia.org/api/search/PrefixSearch?MaxHits=    15&QueryClass=Species&QueryString=Allium'. This request has been blocked; the content must be served over HTTPS.
                       - there is duplicated code, here and in lookup.js
                     */
                    $.ajax({
                        url: "/lookup",
                        data: { MaxHits: resultsCount, QueryClass: typeName, QueryString: request.term + "*" },
                        dataType: "json",
                        timeout: 5000
                    }).done(function(response) {
                        console.log('Done');
                        var topics = [];
                        callback(response.results.map(function (m) {
                            // topics[m.label] = m.uri;
                            return { "label": m.label /* + " - " +
                            cutStringAfterCharacter(m.description, '.') */, "value": m.uri }
                        }))
                    });
                    };
                })
            }
            }
        })
    });
});

function cutStringAfterCharacter(s, c) {
    if (!(s === null)) {
        var n = s.indexOf(c);
        return s.substring(0, n != -1 ? n : s.length);
    } else {
        return s;
    }
};
