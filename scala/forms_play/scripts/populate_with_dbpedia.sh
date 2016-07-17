#!/bin/bash

echo Have you downloaded DBPEDIA with script download-dbpedia.sh ?
echo With this script, you have to start from a virgin database, it uses tdbloader2 instead of tdbloader, see https://jena.apache.org/documentation/tdb/commands.html#tdbloader2
echo Press RETURN
read WAIT

DBPEDIA_VERSION=2015-10
DBPEDIA_DIR=~/data/dbpedia.org/$DBPEDIA_VERSION
DBPEDIA_GRAPH_URI=~/data/dbpedia.org/$DBPEDIA_VERSION

for file in $DBPEDIA_DIR/*.ttl
do
	echo File $file $DBPEDIA_GRAPH_URI
	sed -e"s/.$/<dbpedia:$file>./" $file > $file.nt
	# graphload $file $DBPEDIA_GRAPH_URI
	echo DONE, return code "$?" : created NT file $file.nt with single named graph $DBPEDIA_GRAPH_URI
done

$HOME/src/jena/apache-jena/bin/tdbloader2 --loc ../TDB $DBPEDIA_DIR/*.ttl.nt
