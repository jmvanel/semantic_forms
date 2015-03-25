// https://developer.mozilla.org/fr/docs/Web/API/XMLHttpRequest
// https://github.com/dbpedia/lookup
// http://stackoverflow.com/questions/148901/is-there-a-better-way-to-do-optional-function-parameters-in-javascript
// wget "http://lookup.dbpedia.org/api/search.asmx/PrefixSearch?QueryClass=&MaxHits=10&QueryString=berl" --header='Accept: application/json'

var urlReqPrefix = "http://lookup.dbpedia.org/api/search.asmx/PrefixSearch?QueryClass=&MaxHits=10&QueryString=" ;
var req = new XMLHttpRequest();

function dbpediaComplete( inputElement, callback ) {
  console.log( "dbpediaComplete " + inputElement );
  var word = inputElement.value ;
  console.log( "dbpediaComplete word " + word );
  req.open('GET', urlReqPrefix + word, true);
  req.setRequestHeader('Accept', 'application/json');
  req.onreadystatechange = callback;
  req.send(null);
  console.log( "dbpediaComplete req sent!" );
};

// function installDbpediaComplete( inputElement) {
//   function dbpedia_callback(aEvt) {
//     if (req.readyState == 4) {
//      console.log( "req.response " + req.response );
//      var response= eval( '(' + req.responseText + ')' ); // TODO eval not secure!
//      populate_pulldown_menu( element, response.results );
//     };
//   };
//   console.log( "inputElement " + inputElement );
//   console.log( "getElementsByName(inputElement " + document.getElementsByName(inputElement) );
//   var element = document.getElementsByName(inputElement)[0];
//   element.onkeyup = function() { dbpediaComplete( element, dbpedia_callback); };
// };

function onkeyupComplete( element ) {
  function dbpedia_callback(aEvt) {
    if (req.readyState == 4) {
     console.log( "req.response length " + Object.keys(req.response).length );
     var response= eval( '(' + req.responseText + ')' ); // TODO eval not secure!
     populate_pulldown_menu( element, response.results );
    };
  };
  console.log( "onkeyupComplete " + this );
  dbpediaComplete( element, dbpedia_callback);
};

/** populate pulldown menu with string responses, while keeping associated URI's */
function populate_pulldown_menu( element, results ) {
  var datalist = $(element.list);
  datalist.empty()
  console.log( "datalist " + datalist );
  for (var i in results) {
    var response = results[i];
    // console.log( "response " + response );
    console.log( "response label " + response.label );
    // 	add an option tag to datalist with label=response.label and value=response.uri
    datalist.append(
      jQuery('<option/>', {
        label: response.label + " - " + response.description,
        value: response.uri } ))
  }
};

function test_callback(aEvt) {
  if (req.readyState == 4) {
     if(req.status == 200) {
      dump(req.responseText);
     } else {
      dump("Erreur pendant le chargement de la page.\n"); }
  }
};

function installDbpediaComplete(id) {
    console.log( "installDbpediaComplete " + id );
$("#"+id).select2({
  ajax: {
    url: "http://lookup.dbpedia.org/api/search.asmx/PrefixSearch",
    dataType: 'json',
    delay: 250,
    data: function (params) {
      return {
        MaxHits: 10,
        QueryString: params.term
      };
    },
    processResults: function (data, page) {
      // parse the results into the format expected by Select2.
      // since we are using custom formatting functions we do not need to
      // alter the remote JSON data
      return {
        results: data.results
      };
    },
    cache: true
  },
  escapeMarkup: function (markup) { return markup; }, // let our custom formatter work
  minimumInputLength: 1,
  templateResult: function (result) {
    return result.label + ' - ' + result.description; 
  }, // omitted for brevity, see the source of this page
  templateSelection: function (result) {
    return result.label + ' - ' + result.description; 
  } // omitted for brevity, see the source of this page
});
};
