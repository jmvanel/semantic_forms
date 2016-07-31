#!/bin/bash

for f in lib/*.jar
do
  JARS=$JARS:$f
done

GRAPH=$1
echo "Sortir le graphe '$GRAPH' dans la base TDB"
echo "CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <$GRAPH> { ?s ?p ?o } . }" > /tmp/graphdump.rq

java -cp $JARS tdb.tdbquery --loc=TDB --verbose --query=/tmp/graphdump.rq

