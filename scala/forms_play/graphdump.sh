#!/bin/bash

GRAPH=$1
echo "Imprimer le graphe $GRAPH dans la base TDB"

echo "CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <$GRAPH> { ?s ?p ?o } . }" > /tmp/graphdump.rq

sbt <<EOF
runMain tdb.tdbquery --loc=TDB --verbose --query=/tmp/graphdump.rq
EOF

