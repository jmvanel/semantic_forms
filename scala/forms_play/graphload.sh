#!/bin/bash

URI=$1
GRAPH=$2

MESS="l'URI $URI dans le graphe $GRAPH dans la base TDB."
echo "Charger $MESS"
echo "LOAD <$URI> INTO GRAPH <$GRAPH>" > /tmp/load_graph.rq

sbt <<EOF
runMain tdb.tdbupdate --loc=TDB --verbose --update=/tmp/load_graph.rq
EOF

echo "Local SPARQL database in TDB/ : chargé $MESS."
