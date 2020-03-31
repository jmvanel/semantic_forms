# Delete bad computed labels, cf commit 13a4db6ca5046f0052021bc0c917697a97b72b94

DELETE {
  GRAPH <urn:/semforms/labelsGraphUri/fr> {
    ?S ?P ?O .
} }

WHERE {
  GRAPH <urn:/semforms/labelsGraphUri/fr> {
    ?S ?P ?O . } 
  filter ( STRENDS( ?O, "#"))
}
