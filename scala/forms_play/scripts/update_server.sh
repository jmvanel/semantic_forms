#!/bin/bash

echo "update semantic_forms Play! server when code has changed"
SRC=$HOME/src/semantic_forms/scala/forms_play/
APP=semantic_forms_play
APPVERS=${APP}-1.0-SNAPSHOT
SBT=sbt

cd $SRC
git pull --verbose
$SBT dist
echo "sofware recompiled!"

mkdir ~/deploy
cd ~/deploy
kill `cat ${APPVERS}/RUNNING_PID`

# pour garder les logs
rm -r ${APPVERS}_OLD
mv ${APPVERS} ${APPVERS}_OLD

unzip $SRC/target/universal/${APPVERS}.zip

cd ${APPVERS}
mkdir -p ../TDBsandbox
mkdir -p ../TDBsandbox2
mkdir -p ../TDBsandbox3

ln -s ../TDBsandbox  TDB
ln -s ../TDBsandbox2 TDB
ln -s ../TDBsandbox3 TDB

PORT=9111
echo starting the server on port $PORT
nohup bin/${APP} -J-Xmx100M -J-server -Dhttp.port=$PORT &
