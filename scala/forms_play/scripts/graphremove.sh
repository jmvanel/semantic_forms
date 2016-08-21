#!/bin/bash

GRAPH=$1
echo "Enlever un graphe $GRAPH dans la base TDB"
echo "DROP GRAPH <$GRAPH>" > /tmp/delete_graph.rq

# sbt "runMain tdb.tdbupdate --loc=TDB --verbose --update=/tmp/delete_graph.rq"
sbt <<EOF
runMain tdb.tdbupdate --loc=TDB --verbose --update=/tmp/delete_graph.rq
EOF
echo "Local SPARQL database in TDB/ : Enlevé le graphe $GRAPH."
