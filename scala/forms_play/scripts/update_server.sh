#!/bin/bash

echo "update semantic_forms Play! server when code has changed"
# SRC=$HOME/src/semantic_forms/scala/forms_play/
SRC=$PWD
APP=semantic_forms_play
APPVERS=${APP}-1.0-SNAPSHOT
SBT=sbt

cd $SRC
git pull --verbose

DATE=`date`
MainXml=../forms/src/main/scala/deductions/runtime/views/MainXml.scala
sed -e "s/=timestamp=.*/=timestamp= $DATE/" $MainXml > /tmp/MainXml.scala
cp $MainXml /tmp/MainXml.orig.scala 
cp /tmp/MainXml.scala $MainXml

echo  which $SBT ; which $SBT
echo launching $SBT -J-Xmx2G dist
$SBT -J-Xmx2G dist
SBT_RETURN_CODE=$?
echo $SBT RETURN CODE: $SBT_RETURN_CODE
if test $SBT_RETURN_CODE -ne 0
then echo "Trouble in SBT!" ; exit
else
cp /tmp/MainXml.orig.scala $MainXml
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
ln -s ../TDBsandbox2 TDB2
ln -s ../TDBsandbox3 TDB3

PORT=9111
echo To start the server on port $PORT in directory ~/deploy/$APPVERS , paste this:
echo cd  ~/deploy/$APPVERS \; nohup bin/${APP} -J-Xmx100M -J-server -Dhttp.port=$PORT &
fi

