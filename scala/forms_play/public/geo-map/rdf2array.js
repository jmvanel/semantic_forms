const rdf = require('rdf-ext')
const SparqlStore = require('rdf-store-sparql')
const rdfFetch = require('rdf-fetch')

// cf http://rawgit.com/rdf-ext/rdf-examples/develop/parse-jsonld-to-dataset.html

  let foaf = "http://xmlns.com/foaf/0.1/"
  let rdfs = "http://www.w3.org/2000/01/rdf-schema#"
  let rdfsLabel = rdfs + 'label'
  let geo = ""

/** From a JSON-LD URL , produce a structure like :
 { "data_1" : {"lat": 48.8372728, "long": 2.3353872999999794, "label": "montparnasse"} }
*/
function rdfURL2SimpleArray(url) {
  return rdfFetch( url ).then(
   (res) => {
    console.log( 'FETCH ' + res );
    return res.dataset()
  }).then((dataset) => {

	function rdfsLabelCriterium(quad, subject) {
	 console.log( 'QUAD value ' + quad.predicate.value + ', subject arg ' + subject +
			 ', subject quad ' + quad.subject );
		return quad.predicate.value === rdfsLabel && quad.object.language === 'en'
			&&  quad.subject.toString() == subject.toString() 
	}
    function getRdfsLabel(subj) { return filterQuad(subj, rdfsLabelCriterium) }
    function filterQuad(subj, criterium) {
      console.log('\nsubj ' + subj);
//      console.log('subj match ' + dataset . match(subj, rdf.namedNode(rdfsLabel)) );
      let rdfsLabelQuad = dataset.filter((quad) => {
//    	    console.log( 'QUAD ' + quad );
        return criterium(quad, subj //.toString() 
        		)
        // quad.predicate.value === rdfsLabel && quad.object.language === 'en'
      }).toArray().shift()
      return rdfsLabelQuad && rdfsLabelQuad.object.value;
    };

    console.log( 'FETCH 2 ' + dataset );
    let lats = dataset
    .match(null, rdf.namedNode('http://www.w3.org/2003/01/geo/wgs84_pos#lat'))
    .toArray()
    console.log( 'LATs ' + lats );
    // console.log( 'LAT ' + lats .shift()  .subject.value );

    var subjs = lats.map((latQuad) => {
      console.log('latQuad ' + latQuad);
      return latQuad.subject;
    });
    console.log( 'LAT subjs ' + subjs );
    let rdfsLabelValue = subjs . map((subj) => {
      return getRdfsLabel(subj)
    });
    console.log( 'rdfsLabelValue ' + rdfsLabelValue)
    return lats // TODO <<<<
  })
};


/** UNUSED */
function query2SimpleArray(endpoint, query) {
  // create a new SPARQL store instance pointing to the dbpedia endpoint
  let store = new SparqlStore({ endpointUrl: endpoint });
  // console.log( 'STORE' + store );
      let graph = rdf.graph()
      let stream = store.construct(query)
  console.log( 'STREAM ' + stream );
      // let ret1 =  graph.import(stream)
      // .then(() => { graph });
  let ret1 = stream;
  console.log( 'RET ' + ret1 );
return ret1
};

var endpoint = 'http://semantic-forms.cc:9111/sparql';
// var endpoint = 'http://localhost:9000/sparql';
var query =
'PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ' +
'Prefix geo: <http://www.w3.org/2003/01/geo/wgs84_pos#> ' +
'CONSTRUCT { ?sub geo:long ?LON .?sub geo:lat ?LAT . ?sub rdfs:label ?LAB.} ' +
'WHERE {GRAPH ?GRAPH { ?sub geo:long ?LON .?sub geo:lat ?LAT . ?sub rdfs:label ?LAB.  } } ' ;
// let ret = rdfURL2SimpleArray( endpoint, query );

// ret.on('data', (quad) => { console.log( quad ) })

let url = endpoint + '?query=' + encodeURIComponent(query);
console.log( 'endpoint ' + endpoint);
console.log( 'url ' + url);
let ret =  rdfURL2SimpleArray(url);

console.log( ret );

// ret . catch(function(raison) { /* rejet */ console.log( 'REASON ' + raison ); });
