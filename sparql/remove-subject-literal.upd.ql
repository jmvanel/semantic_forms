# Remove triples with literal subject

DELETE {
  GRAPH ?G {
    ?S ?P ?O .
  }
}

WHERE {
  GRAPH ?G {
    ?S ?P ?O .
    FILTER( isLiteral(?S) )
  }
}
