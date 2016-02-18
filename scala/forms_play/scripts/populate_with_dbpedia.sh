#!/bin/bash

echo Have you downloaded DBPEDIA with script download-dbpedia.sh ?
echo Beware: This script can take hours! You may have to start from a virgin database and use tdbloader2 instead of tdbloader, see https://jena.apache.org/documentation/tdb/commands.html#tdbloader
echo Press RETURN
read ZZ

DBPEDIA_VERSION=2015-04
DBPEDIA_DIR=~/data/dbpedia.org/$DBPEDIA_VERSION

source `dirname $0`/graphload.sh

for file in $DBPEDIA_DIR/*.ttl
do
	echo graphload $file http://dbpedia.org/$DBPEDIA_VERSION
	graphload $file http://dbpedia.org/$DBPEDIA_VERSION
	echo DONE "$?" graphload $file http://dbpedia.org/$DBPEDIA_VERSION
done
