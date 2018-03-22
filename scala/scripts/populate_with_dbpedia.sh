#!/bin/bash

DBPEDIA_VERSION=2016-10
DBPEDIA_DIR=$HOME/data/dbpedia.org/$DBPEDIA_VERSION
DBPEDIA_GRAPH_URI=file://$HOME/data/dbpedia.org/$DBPEDIA_VERSION

JENA=apache-jena-3.6.0
JENA_DIR=$HOME/apps/$JENA


function check_install {
	DIR=$1
	STUFF=$2
	if [ -d "$DIR" ]
	then
		echo "$DIR found."
	else
		echo "$DIR not found, please install $STUFF."
		exit
	fi
}

check_install $JENA_DIR Jena
check_install $DBPEDIA_DIR "DBPEDIA with script download-dbpedia.sh"

echo Have you downloaded DBPEDIA with script download-dbpedia.sh ?
echo With this script, you have to start from a virgin database, it uses tdbloader2 instead of tdbloader, see https://jena.apache.org/documentation/tdb/commands.html#tdbloader2
echo Press RETURN
read WAIT

for file in $DBPEDIA_DIR/*.ttl
do
	echo File $file : convert TTL to N-Triples
	sed -e"s=.$=<dbpedia:$file>.=;/^#/d" $file > $file.nq
	echo DONE, return code "$?" : created NT file $file.nq with single named graph
	ls -l $file.nq
done

echo Start tdbloader2 , create TDB_DBPEDIA/ directory in current dir $PWD
time $JENA_DIR/bin/tdbloader2 --loc TDB_DBPEDIA $DBPEDIA_DIR/*.ttl.nq > /dev/null
# Download Jena from http://archive.apache.org/dist/jena/binaries/apache-jena-3.6.0.tar.gz

echo You may now want to run "./populateRDFCache.sh" after this

