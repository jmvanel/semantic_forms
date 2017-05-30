#!/bin/bash

OLDGRAPH=$1
NEWGRAPH=$2
if [ -z $3 ]; then SUBJECT="?S"; else SUBJECT="<$3>"; fi

echo "change le sujet $SUBJECT dans le graph <$OLDGRAPH> dans le nouveau graph <$NEWGRAPH>"
echo "DELETE { GRAPH <$OLDGRAPH> { $SUBJECT ?P ?O }} INSERT{ GRAPH <$NEWGRAPH> { $SUBJECT ?P ?O }} WHERE {GRAPH <$OLDGRAPH> { $SUBJECT ?P ?O}}" > /tmp/move_subject_graph.rq

# sbt "runMain tdb.tdbupdate --loc=TDB --verbose --update=/tmp/move_subject_graph.rq"
sbt <<EOF
  project forms_play
  runMain tdb.tdbupdate --loc=TDB --verbose --update=/tmp/move_subject_graph.rq
EOF
echo "Local SPARQL database in TDB/ : change le sujet $SUBJECT dans le graph <$OLDGRAPH> dans le nouveau graph <$NEWGRAPH>"
