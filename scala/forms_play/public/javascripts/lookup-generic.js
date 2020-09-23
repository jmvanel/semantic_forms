
const suggestionSearchCSSclass = 'sf-suggestion-search-dbpedia'

function makeProxiedLookupURL(baseURL) {
  return "/proxy?originalurl=" + encodeURIComponent(baseURL)
}
function makeProxiedLookupURLifNecessary(baseURL) {
  if(baseURL.startsWith("http://"))
	  return makeProxiedLookupURL(baseURL)
  else return baseURL
}

/** prepare Completion string, Lucene syntax (for Jena in SF) */
function prepareCompletionLucene( userString ) {
  var stringToSearch = userString
  var words = stringToSearch .split(' ')
  if( words . length > 1 )
    stringToSearch = encodeURIComponent( words[0] + '+AND+' +  words[1] )
  return stringToSearch
}

function registerCompletionGeneric( makeAjaxFunction, lookupCompletionCSSclass, searchServiceURL,
     getRDFtypeInURL, prepareCompletionString ) {
    $(".sf-standard-form").on( 'focus', lookupCompletionCSSclass, function(event) {
	var inputElement = $(this);
        $(this).autocomplete({
            autoFocus: true,
            minlength: 3,
            html: true,

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
                var ajax = makeAjaxFunction( searchServiceURL, request, inputElement, callback,
                                             getRDFtypeInURL, prepareCompletionString )
                console.log(ajax)

                ajax.fail(function (error){
                    console.log("error in Ajax " + error )

/*
                  var ajax = makeAjaxFunction( makeDbPediaLookupURL(alternativeLookupServer), request, inputElement, callback,
                                             getRDFtypeInURL)
                  console.log(ajax)
                  ajax.fail(function (error){

                   // in lookup.js, the same completion is launched on CSS class .sfLookup
                   if( ! inputElement.hasClass('.sfLookup') ) {
                    console.log("lookup.dbpedia.org FAILED: error:" + error.statusText )
                    console.log(error )
                    console.log("lookup.dbpedia.org FAILED => launch local /lookup '" + request.term + "'" )
                    var ajax = makeAjaxFunction( "/lookup", request, inputElement, callback,
		                                 getRDFtypeInURL )
                    console.log(ajax)
                   }
                  })
		*/
                }) // end ajax.fail
            }
          } // end source function
        }) // end autocomplete
    }); // end on focus function
  } // end function registerCompletionGeneric

/**
	compare results: QueryClass=person , and ?QueryClass=place
	view-source:http://lookup.dbpedia.org/api/search/PrefixSearch?QueryClass=Person&QueryString=berlin
	view-source:http://lookup.dbpedia.org/api/search/PrefixSearch?QueryClass=Place&QueryString=berlin

*/
function getRDFtypeInURLastItem(inputElement) {
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
                  typeNameInURL = typeName
                else
                  typeNameInURL = ""
  return typeNameInURL
}

function getRDFtypeInURLfullURI(inputElement) {
  var typeName = getRDFtypeFullURI(inputElement)
  if( typeName.length > 0 )
                  typeNameInURL = typeName
                else
                  typeNameInURL = ""
  return typeNameInURL
}

function getRDFtypeFullURI(inputElement) {
	// QueryClass comes from attribute data-rdf-type in <input> tag , but data-rdf-type is a full URI !
        var typeName = "";
        var $el = inputElement;
        if( $el && $el.attr('data-rdf-type') )
          typeName = $el.attr('data-rdf-type')
  return typeName
}

function cutStringAfterCharacter(s, c) {
    if (s !== null) {
        var n = s.indexOf(c);
        return s.substring(0, n != -1 ? n : s.length);
    } else {
        return "_"
    }
}

