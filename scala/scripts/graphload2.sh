#!/bin/bash

function graphload {
  URL=$1
  GRAPH=$2
  SFDIR=$HOME/src/semantic_forms/scala

  MESS="l'URL $URL dans le graphe $GRAPH dans la base TDB."
  echo "Charger $MESS"
  echo "LOAD <$URL> INTO GRAPH <$GRAPH>" > /tmp/load_graph.rq

  OLD_DIR=$PWD
  cd $SFDIR
  JAVA_OPTS="-Xmx4800m" sbt <<EOF
     project forms_play
     runMain tdb2.tdbupdate --loc=TDB --verbose --update=/tmp/load_graph.rq
EOF

  echo "Local SPARQL database in TDB/ : chargé $MESS."
  cd $OLD_DIR
}

echo graphload "URL" "GRAPH" 
