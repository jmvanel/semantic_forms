# Example of removing triples with given subject
# fill the desired subject URI in variable ?S

# Note: currently /sparql service does NOT allow for binding variables

DELETE {
  GRAPH ?G {
    ?S ?P ?O .
  }
}

WHERE {
  BIND ( <http://semantic-forms.cc:9112/ldp/111-222> AS ?S )

  GRAPH ?G {
    ?S ?P ?O .
  }
}
