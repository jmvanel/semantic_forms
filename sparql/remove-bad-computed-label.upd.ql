# removing triples with bad computed label

DELETE {
  GRAPH <urn:/semforms/labelsGraphUri/fr> {
    ?S ?P ?O .
  }
}

WHERE {
  GRAPH <urn:/semforms/labelsGraphUri/fr> {
   ?S ?P ?O . 
   FILTER ( STRENDS( ?O, '#' ) )
} }
