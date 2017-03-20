#!/bin/bash

echo runMain tdb.tdbdump --loc=TDB  EOF \> dump.nq

sbt "project forms_play" "runMain tdb.tdbdump --loc=TDB" > dump.nq
ls -l dump.nq
echo DONE
