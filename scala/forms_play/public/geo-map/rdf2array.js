// uncomment for use with Node.js

//const rdf = require('rdf-ext')
//const SparqlStore = require('rdf-store-sparql')
//const rdfFetch = require('rdf-fetch')

/* cf
 * http://rawgit.com/rdf-ext/rdf-examples/develop/parse-jsonld-to-dataset.html
 * https://github.com/rdfjs/representation-task-force/blob/master/interface-spec.md */

/*global rdfFetch rdf */

// let foaf = "http://xmlns.com/foaf/0.1/"
let rdfs = "http://www.w3.org/2000/01/rdf-schema#"
let rdfsLabel = rdfs + "label"
let geo = "http://www.w3.org/2003/01/geo/wgs84_pos#"
let geoLong = geo + "long"
let geoLat = geo + "lat"
let displayLabel = "urn:displayLabel"

/** From a JSON-LD URL , produce a structure like :
 * [{"@id":"http://dbpedia.org/resource/Lyon","label":"Lyon",
 *   "long":"4.840000152587891E0","lat":"4.57599983215332E1"}]
 *   as a Promise.
*/
function rdfURL2SimpleArray(/*String: */url)/*: Promise*/ {
  return rdfFetch( url ).then(
      (res) => {
//    console.log( 'FETCH ' + res );
        return res.dataset()
  }).then((dataset) => {

    /** For filtering with RDF lang */
    function rdfsLabelCriterium(quad, subject) {
    return (
       quad.predicate.value === rdfsLabel ||
       quad.predicate.value === displayLabel
       )
     // TODO:	&& quad.object.language === 'en'
     &&  quad.subject.toString() == subject.toString()
}

	/** For filtering with non-lang RDF property */
	function plainPropertyCriterium(/*Quad: */quad, /*Term: */subject, /*String: */property)/*:Boolean*/ {
     return quad.predicate.value === property
       &&  quad.subject.toString() == subject.toString()
	}

	function longCriterium(quad, subject) {
		return plainPropertyCriterium(quad, subject, geoLong) }
	function latCriterium(quad, subject) {
		return plainPropertyCriterium(quad, subject, geoLat) }

	function getRdfsLabel(subj) {
		var rdfsLabelValue = filterQuad(subj, rdfsLabelCriterium)
		var rdfsLabelOrElse = rdfsLabelValue
		if( rdfsLabelValue == "" || rdfsLabelValue == undefined )
			rdfsLabelOrElse = subj.toString()
		return rdfsLabelOrElse

	}
	function getGeoLong(subj) { return filterQuad(subj, longCriterium) }
	function getGeoLat(subj) { return filterQuad(subj, latCriterium) }

	/** filter Quad with given criterium
	 * @return value, e.g. of rdfs:label */
	function filterQuad(subj, criterium) {
      // console.log('\nsubj ' + subj);
      let rdfsLabelQuad = dataset.filter((quad) => {
        return criterium(quad, subj)
      }).toArray().shift()
      return rdfsLabelQuad && rdfsLabelQuad.object.value;
    }

//    console.log( 'RDF FETCH dataset:' ); console.log( dataset );
    let lats = dataset
    .match(null, rdf.namedNode("http://www.w3.org/2003/01/geo/wgs84_pos#lat"))
    .toArray()
//    console.log( 'LATs: ' ); console.log( lats );

    var subjs = lats.map((latQuad) => { return latQuad.subject; });
    console.log( 'LAT subjs ' + subjs )

    let simpleArray = subjs . map((subj) => {
      return {
        "@id": (subj.toString()),
        "label":	getRdfsLabel(subj),
        "long":	getGeoLong(subj),
        "lat":	getGeoLat(subj)
    }
    })
    // console.log( 'rdfURL2SimpleArray: simpleArray: ' + JSON.stringify(simpleArray) )
    return simpleArray
  })
}

/*global test_rdfURL2SimpleArray() */
function test_rdfURL2SimpleArray() {
  var endpoint = 'http://semantic-forms.cc:9111/sparql';
  // var endpoint = 'http://localhost:9000/sparql';
  var query =
    'PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ' +
    'Prefix geo: <http://www.w3.org/2003/01/geo/wgs84_pos#> ' +
    'CONSTRUCT { ?sub geo:long ?LON .?sub geo:lat ?LAT . ?sub rdfs:label ?LAB.} ' +
    'WHERE {GRAPH ?GRAPH { ?sub geo:long ?LON .?sub geo:lat ?LAT . ?sub rdfs:label ?LAB.  } } ' ;
  let url = endpoint + '?query=' + encodeURIComponent(query)
  console.log( 'endpoint ' + endpoint)
  console.log( 'url ' + url)
  let ret =  rdfURL2SimpleArray(url)
  console.log( ret )
}

// uncomment for testing in command line with Node.js
// test_rdfURL2SimpleArray()
