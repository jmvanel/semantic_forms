# Example of removing triples with given subject
# fill the desired subject URI in variable ?O

# Note: currently /sparql service does NOT allow for binding variables

DELETE {
  GRAPH ?G {
    ?S ?P ?O .
  }
}

WHERE {
  BIND ( <file:///home/jmv/deploy/naturalistes/post-07-512.png> AS ?O )

  GRAPH ?G {
    ?S ?P ?O .
  }
}
