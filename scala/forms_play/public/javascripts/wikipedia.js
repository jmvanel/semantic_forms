// https://developer.mozilla.org/fr/docs/Web/API/XMLHttpRequest
// https://github.com/dbpedia/lookup
// http://stackoverflow.com/questions/148901/is-there-a-better-way-to-do-optional-function-parameters-in-javascript
// wget "http://lookup.dbpedia.org/api/search.asmx/PrefixSearch?QueryClass=&MaxHits=10&QueryString=berl" --header='Accept: application/json'

var urlReqPrefix = "http://lookup.dbpedia.org/api/search.asmx/PrefixSearch?QueryClass=&MaxHits=10&QueryString=" ;
function dbpedia_callback(aEvt) {
   var response= eval( req.responseText ); // TODO not secure!
   console.log( "response " + response );
   inputElement.text = response.results.uri; // TODO populate pulldown menu
};

function dbpediaComplete( inputElement, callback ) {
  console.log( "dbpediaComplete " + inputElement );
  var req = new XMLHttpRequest();
  var word = inputElement.text ;
  console.log( "dbpediaComplete word " + word );
  req.open('GET', urlReqPrefix + word, true);
  req.setRequestHeader('Accept', 'application/json');
  req.onreadystatechange = callback;
  req.send(null);
};

function installDbpediaComplete( inputElement) {
  console.log( "inputElement " + inputElement );
  console.log( "getElementByName(inputElement " + document.getElementsByName(inputElement) );
  var element = document.getElementsByName(inputElement)[0];
  element.onChange = function() { dbpediaComplete( element, dbpedia_callback); };
};

function test_callback(aEvt) {
  if (req.readyState == 4) {
     if(req.status == 200) {
      dump(req.responseText);
     } else {
      dump("Erreur pendant le chargement de la page.\n"); }
  }
};
