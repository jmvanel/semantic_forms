#!/bin/bash

for f in lib/*.jar
do
  JARS=$JARS:$f
done

JAVA_OPTS="-Xmx4800m"
java -cp $JARS deductions.runtime.jena.lucene.TextIndexerRDF

