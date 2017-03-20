#!/bin/bash

echo You may now want to run "download-dbpedia.sh" and "populate_with_dbpedia.sh" '(necessarily BEFORE this)'

sbt "project forms_play" "runMain deductions.runtime.sparql_cache.apps.PopulateRDFCache"

