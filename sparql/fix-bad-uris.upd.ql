# SPARQL update that fixes the database:
# removes spaces in URI's

delete { GRAPH ?G {
  ?S ?P ?O .
} }
insert { GRAPH ?G {
  ?S ?P ?OnewURI .
  ?SnewURI ?P ?O .
} }
WHERE {
GRAPH ?G {
{ ?S ?P ?O .
  FILTER ( isURI(?O) ) .
  FILTER ( contains( str(?O), " " ) ) .
  BIND ( URI(replace( str(?O), " ", "")) AS ?OnewURI )
} UNION
{ ?S ?P ?O .
  FILTER ( isURI(?S) ) .
  FILTER ( contains( str(?S), " " ) ) .
  BIND ( URI(replace( str(?S), " ", "")) AS ?SnewURI )
}
} }
