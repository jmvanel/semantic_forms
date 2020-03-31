# Count bad computed labels, cf commit 13a4db6ca5046f0052021bc0c917697a97b72b94
SELECT ?S ( (COUNT(?O)) as ?co ) WHERE {
  GRAPH <urn:/semforms/labelsGraphUri/fr>
    { ?S ?P ?O . } 
  filter ( STRENDS( ?O, "#"))
} GROUP BY ?S
