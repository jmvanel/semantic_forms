# Example of removing unwanted triples 

PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>

DELETE {
  # GRAPH ?G {
  GRAPH <https://w3id.org/scholarlydata/ISWC/2017> {
    ?S rdfs:range rdfs:Resource .
  }
}

WHERE {
  GRAPH ?G {
    ?S rdfs:range rdfs:Resource .
  }
}
