#!/bin/bash

for f in lib/*.jar
do
  JARS=$JARS:$f
done
java -cp $JARS deductions.runtime.jena.lucene.TextIndexerRDF

echo Lucene indexing done in LUCENE/
du -s LUCENE
