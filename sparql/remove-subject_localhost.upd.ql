# Remove triples starting with http://localhost
# CAUTION: do not execute on localhost server !

DELETE {
  GRAPH ?G {
    ?S ?P ?O .
  }
}

WHERE {
  GRAPH ?G {
    ?S ?P ?O .
    FILTER( STRSTARTS(STR(?S), "http://localhost") )
  }
}
