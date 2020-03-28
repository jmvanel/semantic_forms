# FIX triples with bad xsd:time literal : missing seconds part
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
DELETE {
  GRAPH ?G {
    ?S ?P ?O .
} }
INSERT {
  GRAPH ?G {
    ?S ?P ?TIME_DT .
} }
WHERE {
  GRAPH ?G {
    ?S ?P ?O .
    filter ( xsd:time=datatype(?O) )
    filter ( regex( str(?O), '^[0-9][0-9]:[0-9][0-9]' )  )
    BIND ( CONCAT( ?O, ":00" ) AS ?TIME )
    BIND ( xsd:time(?TIME) AS ?TIME_DT )
  }
}
