# Example of removing triples with given subject
# fill the desired subject URI in variable ?DEL

# Note: currently /sparql service does NOT allow for binding variables

DELETE {
  GRAPH ?G {
    ?S ?P ?DEL .
  }
  GRAPH ?GR {
    ?DEL ?P1 ?O1 .
  }
}

WHERE {
  BIND ( <file:///home/jmv/deploy/naturalistes/post-07-512.png> AS ?DEL )

  {
  GRAPH ?G {
    ?S ?P ?DEL .
  }
  } UNION {
  GRAPH ?GR {
    ?DEL ?P1 ?O1 .
  }
  }
}
