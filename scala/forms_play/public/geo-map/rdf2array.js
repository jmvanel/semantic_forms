const rdf = require('rdf-ext')
const SparqlStore = require('rdf-store-sparql')
const rdfFetch = require('rdf-fetch')

/* cf
 * http://rawgit.com/rdf-ext/rdf-examples/develop/parse-jsonld-to-dataset.html
 * https://github.com/rdfjs/representation-task-force/blob/master/interface-spec.md */

  let foaf = "http://xmlns.com/foaf/0.1/"
  let rdfs = "http://www.w3.org/2000/01/rdf-schema#"
  let rdfsLabel = rdfs + 'label'
  let geo = "http://www.w3.org/2003/01/geo/wgs84_pos#"
  let geoLong = geo + 'long'
  let geoLat = geo + 'lat'

/** From a JSON-LD URL , produce a structure like :
 * TODO:
 { "data_1" : {"lat": 48.8372728, "long": 2.3353872999999794, "label": "montparnasse"} }
DONE:
 simpleArray [{"label":"Grenoble","long":"5.7222","lat":"45.2002"},{"label":"Paris","long":"2.3508","lat":"48.8567"}]
*/
function rdfURL2SimpleArray(url) {
  return rdfFetch( url ).then(
   (res) => {
//    console.log( 'FETCH ' + res );
    return res.dataset()
  }).then((dataset) => {

	/** For filtering with RDF lang */
	function rdfsLabelCriterium(quad, subject) {
	 return quad.predicate.value === rdfsLabel && quad.object.language === 'en'
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

	function getRdfsLabel(subj) { return filterQuad(subj, rdfsLabelCriterium) }
	function getGeoLong(subj) { return filterQuad(subj, longCriterium) }
	function getGeoLat(subj) { return filterQuad(subj, latCriterium) }

	/** filter Quad with given criterium
	 * @return value, e.g. of rdfs:label */
	function filterQuad(subj, criterium) {
      console.log('\nsubj ' + subj);
      let rdfsLabelQuad = dataset.filter((quad) => {
        return criterium(quad, subj)
      }).toArray().shift()
      return rdfsLabelQuad && rdfsLabelQuad.object.value;
    };

//    console.log( 'FETCH 2 ' + dataset );
    let lats = dataset
    .match(null, rdf.namedNode('http://www.w3.org/2003/01/geo/wgs84_pos#lat'))
    .toArray()
//    console.log( 'LATs ' + lats );

    var subjs = lats.map((latQuad) => { return latQuad.subject; });
    console.log( 'LAT subjs ' + subjs )

    let simpleArray = subjs . map((subj) => {
      return {
    	  "label":	getRdfsLabel(subj),
    	  "long":	getGeoLong(subj),
    	  "lat":	getGeoLat(subj)
    	  }
    })
    console.log( 'simpleArray ' + JSON.stringify(simpleArray) )
    return simpleArray
  })
};

function test_rdfURL2SimpleArray() {
  var endpoint = 'http://semantic-forms.cc:9111/sparql';
  // var endpoint = 'http://localhost:9000/sparql';
  var query =
'PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ' +
'Prefix geo: <http://www.w3.org/2003/01/geo/wgs84_pos#> ' +
'CONSTRUCT { ?sub geo:long ?LON .?sub geo:lat ?LAT . ?sub rdfs:label ?LAB.} ' +
'WHERE {GRAPH ?GRAPH { ?sub geo:long ?LON .?sub geo:lat ?LAT . ?sub rdfs:label ?LAB.  } } ' ;

  let url = endpoint + '?query=' + encodeURIComponent(query);
  console.log( 'endpoint ' + endpoint);
  console.log( 'url ' + url);
  let ret =  rdfURL2SimpleArray(url);
  console.log( ret );
}

test_rdfURL2SimpleArray()

