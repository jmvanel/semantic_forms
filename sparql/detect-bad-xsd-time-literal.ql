# Detect triples with bad xsd:time literal : missing seconds part

PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT * WHERE { GRAPH ?G {
  # ?S ?P "10:00"^^<http://www.w3.org/2001/XMLSchema#time> .
  ?S ?P ?O .
  filter ( datatype(?O) = xsd:time )
  filter ( regex(str(?O), '^[0-9][0-9]:[0-9][0-9]' ) )
} } LIMIT 200
