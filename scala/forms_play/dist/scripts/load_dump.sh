#!/bin/bash

DIR=$1
echo Re-load dump made with dump.sh in directory $DIR
echo local TDB directories in $PWD will be deleted and recreated
echo Press RETURN
read WAIT

rm -r TDB TDB2 TDB3

JENA=apache-jena-3.6.0

$HOME/apps/$JENA/bin/tdbloader2 --loc TDB $DIR/dump.nq
	echo 'DONE dump.nq --> TDB' ; echo
$HOME/apps/$JENA/bin/tdbloader2 --loc TDB2 $DIR/dump_timestamps.nq
	echo 'DONE dump_timestamps.nq --> TDB2' ; echo
$HOME/apps/$JENA/bin/tdbloader2 --loc TDB3 $DIR/dump_accounts.nq
	echo 'DONE dump_accounts.nq --> TDB3' ; echo

# Download Jena from http://archive.apache.org/dist/jena/binaries/apache-jena-2.13.0.tar.gz
# $HOME/apps/apache-jena-2.13.0/bin/tdbloader2 --loc $SFDIR/TDB $DBPEDIA_DIR/*.ttl.nq

echo DONE re-load dumps dump.nq , dump_timestamps.nq , dump_accounts.nq

