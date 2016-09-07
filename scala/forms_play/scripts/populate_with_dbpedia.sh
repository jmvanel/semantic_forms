#!/bin/bash

DBPEDIA_VERSION=2015-10
DBPEDIA_DIR=$HOME/data/dbpedia.org/$DBPEDIA_VERSION
DBPEDIA_GRAPH_URI=file://$HOME/data/dbpedia.org/$DBPEDIA_VERSION
JENA=apache-jena-3.1.0
JENA_DIR=$HOME/apps/$JENA

function check_install {
	DIR=$1
	STUFF=$2
	if [ -f "$DIR" ]
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
	echo File $file
	sed -e"s=.$=<dbpedia:$file>.=;/^#/d" $file > $file.nq
	echo DONE, return code "$?" : created NT file $file.nq with single named graph
	ls -l $file.nq
done

$JENA_DIR/bin/tdbloader2 --loc TDB $DBPEDIA_DIR/*.ttl.nq
# Download Jena from http://archive.apache.org/dist/jena/binaries/apache-jena-3.1.0.tar.gz

echo You may now want to run "scripts/populateRDFCache.sh" after this

