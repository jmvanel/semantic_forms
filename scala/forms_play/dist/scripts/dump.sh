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
java -cp $JARS tdb.tdbdump --loc=TDB 1> dump.nq
ls -l dump.nq
echo DONE quads dump - edit dump.nq to remove log messages

