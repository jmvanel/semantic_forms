#!/bin/bash

function graphload {
  URI=$1
  GRAPH=$2
  SFDIR=$HOME/src/semantic_forms/scala/forms_play

  MESS="l'URI $URI dans le graphe $GRAPH dans la base TDB."
  echo "Charger $MESS"
  echo "LOAD <$URI> INTO GRAPH <$GRAPH>" > /tmp/load_graph.rq

  OLD_DIR=$PWD
  cd $SFDIR
  JAVA_OPTS="-Xmx4800m" sbt <<EOF
     project forms_play
     runMain tdb.tdbupdate --loc=TDB --verbose --update=/tmp/load_graph.rq
EOF

  echo "Local SPARQL database in TDB/ : chargé $MESS."
  cd $OLD_DIR
}

# graphload $1, $2 
