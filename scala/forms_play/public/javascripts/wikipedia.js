// https://developer.mozilla.org/fr/docs/Web/API/XMLHttpRequest
// https://github.com/dbpedia/lookup
// http://stackoverflow.com/questions/148901/is-there-a-better-way-to-do-optional-function-parameters-in-javascript
// wget "http://lookup.dbpedia.org/api/search.asmx/PrefixSearch?QueryClass=&MaxHits=10&QueryString=berl" --header='Accept: application/json'

var urlReqPrefix = "http://lookup.dbpedia.org/api/search.asmx/PrefixSearch?QueryClass=&MaxHits=10&QueryString="

function installDbpediaComplete( inputElement) {
  // inputElement refers to attribute	<input name="
  // on user input launch completion ??????
  inputElement.onChange = dbpediaComplete( inputElement )
};

function dbpediaComplete( inputElement, callback ) {
  var req = new XMLHttpRequest();
  callback = (typeof callback === "undefined") ? dbpedia_callback : callback
  val word = inputElement.text
  req.open('GET', urlReqPrefix + word, true);
  // 'Accept: application/json'
  req.setRequestHeader('Accept', 'application/json');
  req.onreadystatechange = callback;

  req.send(null);

  function dbpedia_callback(aEvt) {
   // decode req.responseText
   response = eval( req.responseText ) // TODO not secure!
   // update inputElement
  inputElement.text = response.results.uri // TODO populate pulldown menu
  };
};

function test_callback(aEvt) {
  if (req.readyState == 4) {
     if(req.status == 200)
      dump(req.responseText);
     else
      dump("Erreur pendant le chargement de la page.\n");
  }
};
