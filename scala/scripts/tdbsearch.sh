#!/bin/bash

SEARCH=$1
echo "Chercher une chaîne et afficher les triplets avec le graphe dans la base TDB"
echo "Trace a given string (regex actually) and the named graph where the triples are."

# echo "SELECT DISTINCT ?s ?p ?o ?g WHERE { GRAPH ?g { ?s ?p ?o } . FILTER regex( ?o, '${SEARCH}', 'i') }" > /tmp/tdbsearch.rq
echo " SELECT DISTINCT ?s ?p ?o ?g WHERE { GRAPH ?g { ?s ?p ?o } . OPTIONAL { ?s ?p ?o } FILTER regex( ?o, '${SEARCH}', 'i') } " > /tmp/tdbsearch.rq
sbt <<EOF
  project forms_play
  runMain tdb.tdbquery --loc=TDB --verbose --query=/tmp/tdbsearch.rq
EOF


