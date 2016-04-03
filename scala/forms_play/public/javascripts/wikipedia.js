// https://developer.mozilla.org/fr/docs/Web/API/XMLHttpRequest
// https://github.com/dbpedia/lookup
// http://stackoverflow.com/questions/148901/is-there-a-better-way-to-do-optional-function-parameters-in-javascript
// wget "http://lookup.dbpedia.org/api/search.asmx/PrefixSearch?QueryClass=&MaxHits=10&QueryString=berl" --header='Accept: application/json'

/* TODO pulldown menu in <input> does not show on Chrome and Opera
see http://blog.teamtreehouse.com/creating-autocomplete-dropdowns-datalist-element 
https://github.com/RubenVerborgh/dbpedia-lookup-page
https://github.com/scala-js/scala-js-jquery
https://www.google.fr/search?q=ajax+example+scala.js
 */

var resultsCount = 20;
var urlReqPrefix = "http://lookup.dbpedia.org/api/search.asmx/PrefixSearch?QueryClass=&MaxHits=" +
      resultsCount + "&QueryString=" ;
// var urlReqPrefix = "/lookup?q=";
var req = new XMLHttpRequest();

/** onkeyup callback on <input> tag */
function onkeyupComplete( element ) {
//  function dbpedia_callback(aEvt) {
//    if (req.readyState == 4) {
//     console.log( "req.response length " + Object.keys(req.response).length );
//     var response= eval( '(' + req.responseText + ')' ); // TODO eval not secure!
//     populate_pulldown_menu( element, response.results );
//    };
//  };
  console.log( "onkeyupComplete " + this );
//  dbpediaComplete( element, dbpedia_callback);
};

function addDBPediaLookup( inputElementId ) {
      console.log( "addDBPediaLookup " + inputElementId );
      console.log( "addDBPediaLookup " + " " + (typeof $(inputElementId)) );
      // var inputElement = $(inputElementId);
      var inputElement = document.getElementById(inputElementId.substring(1));
      var topics = {}, // NOTE topics is populated but not used 
          $topics = $(inputElementId).autocomplete({
        autoFocus: true,
       select: function( event, ui ) {
            console.log( "Topic chosen label event " + (event) );
            console.log( "Topic chosen label ui" + JSON.stringify(ui) );
            console.log( "Topic chosen label " + ui.item.label +
              ", topics[ui.label] " + topics[ui.item.label] +
              ", topics[ui.value] " + topics[ui.item.value] +
              ", ui.value " + ui.item.value );
            console.log( "typeof inputElement " + typeof inputElement );
            console.log( "  inputElement.value ", inputElement.value );
            // inputElement.value = topics[ui.item.label];
            // $topics.value = topics[ui.item.label];
       },
        source: function(request, callback) {
          $.ajax({
            url: "http://lookup.dbpedia.org/api/search/PrefixSearch",
            data: { MaxHits: 15, QueryString: request.term },
            dataType: "json",
            success: function (response) {
              callback(response.results.map(function (m) {
                console.log( "response " + m );
                topics[m.label] = m.uri;
                return { "label": m.label, "value": m.uri }  // m.label;
              }));
            }
          });
        }
      }).keyup(function (event) {
        console.log( "Topic typed-0" );
        var label = $topics.val();
        if (label) {
          console.log( "Topic typed " + label + " " + topics[label]);
        }
      });
};

