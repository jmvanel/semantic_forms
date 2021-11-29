// uncomment for use with Node.js

//const rdf = require('rdf-ext')
//const SparqlStore = require('rdf-store-sparql')
//const rdfFetch = require('rdf-fetch')

/* Create an array of data from the geographic RDF triples
 *
 * cf
 * http://rawgit.com/rdf-ext/rdf-examples/develop/parse-jsonld-to-dataset.html
 * https://github.com/rdfjs/representation-task-force/blob/master/interface-spec.md */

/*global rdfFetch rdf */

let foaf = "http://xmlns.com/foaf/0.1/"
let rdfs = "http://www.w3.org/2000/01/rdf-schema#"
let rdfsLabel = rdfs + "label"
let geo = "http://www.w3.org/2003/01/geo/wgs84_pos#"
let geoLong = geo + "long"
let geoLat = geo + "lat"
let displayLabel = "urn:displayLabel"
let foafDepictionURI = foaf + "depiction"
let foafImgURI = foaf + "img"
let ogcAsWKT = "http://www.opengis.net/ont/geosparql#asWKT"

/** From a JSON-LD URL , produce a structure like :
 * [{"@id":"http://dbpedia.org/resource/Lyon", "label":"Lyon",
 *   "long":"4.840000152587891E0", "lat":"4.57599983215332E1"}]
 *
 * as a Promise.
*/
function rdfURL2SimpleArray(/*String: */url)/*: Promise*/ {
  return rdfFetch( url ).then(
      (res) => {
//    console.log( 'FETCH ' + res );
        return res.dataset()
  }).then((dataset) => {

    /** For filtering with RDF lang */
    function rdfsLabelCriterium(quad, subject) {
      var userLanguage = (navigator.language || navigator.userLanguage ). substring(0,2)
      var ret = (
        quad.predicate.value === rdfsLabel ||
        quad.predicate.value === displayLabel
      )
       && ( quad.object.language === userLanguage ||
            quad.object.language === "" )
       &&  quad.subject.toString() == subject.toString()
//      console.log( 'rdfsLabelCriterium quad: ' + quad)
      return ret
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
    function wktCriterium(quad, subject) {
		return plainPropertyCriterium(quad, subject, ogcAsWKT) }

    function getRdfsLabel(subj) {
		var rdfsLabelValue = filterQuad(subj, rdfsLabelCriterium)
		var rdfsLabelOrElse = rdfsLabelValue
		if( rdfsLabelValue == "" || rdfsLabelValue == undefined )
			rdfsLabelOrElse = subj.toString()
		return rdfsLabelOrElse
	}

    function imgCriterium(quad, subject) {
      return plainPropertyCriterium(quad, subject, foafImgURI ) }
	function depictionCriterium(quad, subject) {
      return plainPropertyCriterium(quad, subject, foafDepictionURI ) }

    function getGeoLong(subj){
      let geoLong = filterQuad(subj, longCriterium)
      if( geoLong === undefined) {
        let wkt = filterQuad(subj, wktCriterium)
        let ret = wkt .
             split(',')[0].
                   replace("POINT(", "")
        return ret
      } else
        return geoLong;
    }
    function getGeoLat(subj) {
      let geo = filterQuad(subj, latCriterium)
      if( geo === undefined) {
        let wkt = filterQuad(subj, wktCriterium)
       return wkt .
            split(',')[1] .
              replace(",0)", "") .
              replace(")", "")
      } else
          return geo;
    }
    function getImage(subj)  { return filterQuad(subj, imgCriterium) ||
                                      filterQuad(subj, depictionCriterium) }

    /** filter Quad with given criterium
     * @return string value, e.g. of rdfs:label , or undefined */
    function filterQuad(subj, criterium) {
      // console.log('subj ' + subj);
      let criteriumQuads = dataset.filter((quad) => {
        return criterium(quad, subj)
      }).toArray().shift()
      // console.log('\n criteriumQuads ' + criteriumQuads );
      return criteriumQuads && criteriumQuads.object.value;
    }

    // console.log( 'RDF FETCH dataset:' ); console.log( dataset );
    let lats = dataset
    .match(null, rdf.namedNode( geoLat ))
    .toArray()
    // console.log( 'LATs: ' ); console.log( lats );
    let wtks = dataset
    .match(null, rdf.namedNode( ogcAsWKT ))
    .toArray()
    // console.log( 'WTKs: ' ); console.log( wtks );

    var subjs = lats.concat(wtks).map( (latQuad) => { return latQuad.subject; });
    console.log( 'LAT + WTK subjs ' ); console.log( subjs )

    let simpleArray = subjs . map((subj) => {
      return {
        "@id": (subj.toString()),
        "label":getRdfsLabel(subj),
        "long": Number( getGeoLong(subj) ),
        "lat":  Number( getGeoLat(subj) ),
        "img":  getImage(subj)
        // 'http://commons.wikimedia.org/wiki/Special:FilePath/Trevoux-008.JPG?width=300'
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
