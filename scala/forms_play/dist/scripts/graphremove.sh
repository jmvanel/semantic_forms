#!/bin/bash

if [ -z "$1" ]; then
  echo 'Argument: GRAPH'
  exit
fi

for f in lib/*.jar
do
  JARS=$JARS:$f
done

GRAPH=$1
echo "Enlever un graphe $GRAPH dans la base TDB"
echo "DROP GRAPH <$GRAPH>" > /tmp/delete_graph.rq

java -cp $JARS tdb.tdbupdate --loc=TDB --verbose --update=/tmp/delete_graph.rq

echo "Local SPARQL database in TDB/ : Removed (Enlevé) graph $GRAPH."
