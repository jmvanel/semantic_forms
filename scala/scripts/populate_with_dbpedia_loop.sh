#!/bin/bash

echo Have you downloaded DBPEDIA with script download-dbpedia.sh ?
echo Beware: This script can take hours! You may have to start from a virgin database and use tdbloader2 instead of tdbloader, see https://jena.apache.org/documentation/tdb/commands.html#tdbloader
echo Press RETURN
read WAIT

DBPEDIA_VERSION=2016-10
DBPEDIA_DIR=~/data/dbpedia.org/$DBPEDIA_VERSION
DBPEDIA_GRAPH_URI=~/data/dbpedia.org/$DBPEDIA_VERSION

source `dirname $0`/graphload.sh

for file in $DBPEDIA_DIR/*.ttl
do
	echo graphload $file $DBPEDIA_GRAPH_URI
	graphload $file $DBPEDIA_GRAPH_URI
	echo DONE "$?" graphload $DBPEDIA_GRAPH_URI
done
