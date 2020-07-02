/* jshint -W014 */
/* jshint -W033 */
/* JS hint "laxcomma": true */
// require("jquery-ui")

$(document).ready(function() {

const lookupCompletionCSSclass = '.sfLookup'
const suggestionSearchCSSclass = 'sf-suggestion-search-sf';
var topics = [];

$("form").on( 'focus', lookupCompletionCSSclass, function(event) {
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
                // console.log( "Topic chosen label event "); console.log($(this));
                console.log( "Topic chosen label ui"); console.log(ui);
                $emptyFields = $(this).siblings().filter(function(index) { return $(this).val() == ''}).length;
                console.log('Champs vides : '+ $emptyFields);
                if ($emptyFields === 0) {
                    /* fails on class creation input, and anyway does not work anymore
                     * (goal is to add a fresh new input field when user has chosen in the menu */
                    // addedWidget = cloneWidget($(this))
                }
            },

            source: function(request, callback) {
		var inputElementContainsURL =
			request .term. startsWith("http://") ||
			request .term. startsWith("urn:") ;
		if( inputElementContainsURL )
			// remove progression indicator because user has already selected
			inputElement.removeClass(suggestionSearchCSSclass);
		else {
		console.log("Déclenche l'événement /lookup pour " + request.term);
                var typeName;
                var $el = $(event.target);
                typeName = $el.attr('data-rdf-type');
                console.log('typeName "' + typeName + '"')

                var stringToSearch = request.term
                var words = stringToSearch .split(' ')
                if( words . length > 1 )
                  stringToSearch = words[0] + ' AND ' +  words[1]

                $.ajax({
                    url: "/lookup",
                    data: { QueryClass: typeName, QueryString: stringToSearch },
                    dataType: "json",
                    timeout: 10000
                }).done(function(response) {
                    console.log('/lookup Done');
                    var topics = [];
                    callback(response.results.map(function (m) {
                        console.log("response.result");
                        console.log(m);
                        // topics[m.label] = m.uri;
                        return { "label": m.label
			+ " - " + cutStringAfterCharacter(m.description, '.')
			+  " - " + m.type
			+  " - refCount " + m.refCount
			+  "  <" + m.uri + ">"
			, "value": m.uri }
                    }))
                }).fail(function (error){
			console.log("/lookup FAILED for " + request.term + ", error: " );
			console.log(error.statusText);
			console.log(error);
		});
            }
            }
        })
    });
})

function cutStringAfterCharacter(s, c) {
    if (s !== null) {
        var n = s.indexOf(c);
        return s.substring(0, n != -1 ? n : s.length);
    } else {
        return s;
    }
}
