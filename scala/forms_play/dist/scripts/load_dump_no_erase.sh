#!/bin/bash

###############################################################################
# This script is usefull when the application has been packaged with "sbt dist"
# (see README.md), and the resulting zip has been unzipped on the target server.
###############################################################################
for f in lib/*.jar
do
  JARS=$JARS:$f
done
echo java -cp $JARS

DIR=$1
echo Re-load dump made with dump.sh in directory $DIR
echo local TDB directories in $PWD will be updated

java -cp $JARS tdb.tdbloader --loc TDB $DIR/dump.nq
        echo 'DONE dump.nq --> TDB' ; echo
java -cp $JARS tdb.tdbloader --loc TDB2 $DIR/dump_timestamps.nq
        echo 'DONE dump_timestamps.nq --> TDB2' ; echo
java -cp $JARS tdb.tdbloader --loc TDB3 $DIR/dump_accounts.nq
        echo 'DONE dump_accounts.nq --> TDB3' ; echo

echo DONE re-load dumps dump.nq , dump_timestamps.nq , dump_accounts.nq

