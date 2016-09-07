#!/bin/bash

for f in lib/*.jar
do
  JARS=$JARS:$f
done
java -cp $JARS deductions.runtime.sparql_cache.PopulateRDFCache

echo You may now want to run "scripts/start.sh"
