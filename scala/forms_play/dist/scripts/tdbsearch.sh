#!/bin/bash

if [ -z "$1" ]; then
    echo 'search string in "TDB" TDB database'
    echo 'tdbsearch.sh search_string'
    exit 1
fi

for f in lib/*.jar
do
  JARS=$JARS:$f
done

SEARCH=$1
echo "Chercher une chaîne et afficher les triplets avec le graphe dans la base TDB"
echo "Trace a given string (regex actually) and the named graph where the triples are."

# echo "SELECT DISTINCT ?s ?p ?o ?g WHERE { GRAPH ?g { ?s ?p ?o } . FILTER regex( ?o, '${SEARCH}', 'i') }" > /tmp/tdbsearch.rq
echo " SELECT DISTINCT ?s ?p ?o ?g WHERE { GRAPH ?g { ?s ?p ?o } . OPTIONAL { ?s ?p ?o } FILTER regex( ?o, '${SEARCH}', 'i') } " > /tmp/tdbsearch.rq
java -cp $JARS tdb.tdbquery --loc=TDB --verbose --query=/tmp/tdbsearch.rq


