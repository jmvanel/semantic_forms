#!/bin/bash

echo Have you downloaded DBPEDIA with script download-dbpedia.sh ?
echo With this script, you have to start from a virgin database, it uses tdbloader2 instead of tdbloader, see https://jena.apache.org/documentation/tdb/commands.html#tdbloader2
echo Press RETURN
read WAIT

DBPEDIA_VERSION=2015-10
DBPEDIA_DIR=$HOME/data/dbpedia.org/$DBPEDIA_VERSION
DBPEDIA_GRAPH_URI=file://$HOME/data/dbpedia.org/$DBPEDIA_VERSION
SFDIR=$HOME/src/semantic_forms/scala/forms_play

for file in $DBPEDIA_DIR/*.ttl
do
	echo File $file $DBPEDIA_GRAPH_URI
	sed -e"s=.$=<dbpedia:$file>.=;/^#/d" $file > $file.nq
	echo DONE, return code "$?" : created NT file $file.nq with single named graph $DBPEDIA_GRAPH_URI
	ls -l $file.nq
done

$HOME/apps/apache-jena-3.0.1/bin/tdbloader2 --loc $SFDIR/TDB $DBPEDIA_DIR/*.ttl.nq
# Download Jena from http://archive.apache.org/dist/jena/binaries/apache-jena-2.13.0.tar.gz
# $HOME/apps/apache-jena-2.13.0/bin/tdbloader2 --loc $SFDIR/TDB $DBPEDIA_DIR/*.ttl.nq
