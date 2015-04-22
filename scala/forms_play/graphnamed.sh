#!/bin/bash

echo "Lister les graphes dans la base TDB"
echo "SELECT DISTINCT ?g WHERE { GRAPH ?g { ?s ?p ?o } . }" > /tmp/graphs.rq
echo "If you have EULERGUI or Jena TDB installed, can also run with java -cp \$EULERGUI_JAR ..."

sbt <<EOF
runMain tdb.tdbquery --loc=TDB --verbose --query=/tmp/graphs.rq
EOF
