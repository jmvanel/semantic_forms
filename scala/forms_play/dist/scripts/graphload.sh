#!/bin/bash

for f in lib/*.jar
do
  JARS=$JARS:$f
done

function graphload {
  URI=$1
  GRAPH=$2

  MESS="URI $URI in graph $GRAPH in database TDB."
  echo "Charger $MESS"
  echo "LOAD <$URI> INTO GRAPH <$GRAPH>" > /tmp/load_graph.rq

  JAVA_OPTS="-Xmx4800m"
  java -cp $JARS tdb.tdbupdate --loc=TDB --verbose --update=/tmp/load_graph.rq

  echo "Local SPARQL database in TDB/ : chargé $MESS."
}

# graphload $1, $2 
