// https://github.com/dbpedia/lookup
// wget "http://lookup.dbpedia.org/api/search.asmx/PrefixSearch?QueryClass=&MaxHits=10&QueryString=berl" --header='Accept: application/json'

/* pulldown menu in <input> does show on Chrome; Opera, Android ????
see
https://jqueryui.com/autocomplete/
https://github.com/RubenVerborgh/dbpedia-lookup-page
alternate implementation, abandoned: http://blog.teamtreehouse.com/creating-autocomplete-dropdowns-datalist-element 
TODO: translate in Scala:
https://github.com/scala-js/scala-js-jquery
https://www.google.fr/search?q=ajax+example+scala.js
 */

var resultsCount = 15;
var urlReqPrefix = "http://lookup.dbpedia.org/api/search.asmx/PrefixSearch?QueryClass=&MaxHits=" +
      resultsCount + "&QueryString=" ;
// var urlReqPrefix = "/lookup?q=";

/** onkeyup callback on <input> tag 
function onkeyupComplete( element ) {
  console.log( "onkeyupComplete " + this );
};
*/

function addDBPediaLookup( inputElementURI ) {
      console.log( "addDBPediaLookup " + inputElementURI );
      console.log( "addDBPediaLookup typeof " + " " + (typeof $(inputElementURI)) );
      var inputElementId = inputElementURI.substring(1);
      var inputElement = document.getElementById(inputElementId);
      inputElement.hasLookup = true;
      var topics = {}, // NOTE topics is populated but not used 
          $topics = $(inputElementURI).autocomplete({
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
            console.log( "  inputElementURI ", inputElementURI );
            cloneWidget(inputElementId);
       },
        source: function(request, callback) {
          $.ajax({
            url: "http://lookup.dbpedia.org/api/search/PrefixSearch",
            data: { MaxHits: resultsCount, QueryString: request.term },
            dataType: "json",
            success: function (response) {
              callback(response.results.map(function (m) {
                console.log( "response " + m );
                topics[m.label] = m.uri;
                return { "label": m.label + " - " +
                          cutStringAfterCharacter(m.description, '.'), "value": m.uri }
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

function cutStringAfterCharacter( s, c) {
  var n = s.indexOf(c);
  return s.substring(0, n != -1 ? n : s.length);
};

