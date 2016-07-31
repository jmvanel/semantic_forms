#!/bin/bash

sbt > dump.nq <<EOF
runMain tdb.tdbdump --loc=TDB
EOF
ls -l dump.nq
echo DONE quads dump - edit dump.nq to remove log messages

