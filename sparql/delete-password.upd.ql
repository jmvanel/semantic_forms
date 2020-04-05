# Delete user password association

DELETE {
graph <urn:users> {
    ?USER ?P ?O .
  }
}
WHERE {
graph <urn:users> {
    ?USER ?P ?O .
    FILTER ( CONTAINS( str(?USER), "jsmith") )
  }
}
