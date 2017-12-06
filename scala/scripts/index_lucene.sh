#!/bin/bash

JAVA_OPTS="-Xmx4800m"
du -sh LUCENE
rm -r LUCENE
sbt "project forms_play" "runMain deductions.runtime.jena.lucene.TextIndexerRDF" 

echo Lucene indexing done in LUCENE/
du -sh LUCENE
