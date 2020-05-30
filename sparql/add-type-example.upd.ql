INSERT {
 GRAPH <taxref:/speciesType/12.0> {
  ?S a <http://dbpedia.org/ontology/Species> .
} }
WHERE {
 GRAPH ?grTaxref {
  ?S <http://taxref.mnhn.fr/lod/property/hasRank> <http://taxref.mnhn.fr/lod/taxrank/Species> .
} }
