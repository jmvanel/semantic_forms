#!/bin/bash

function make_shared_file {
  if [ $(getent group sf) ]; then
    DIR=$1
    sudo chgrp -hR sf $DIR
    sudo chmod -R g+rwx $DIR
  fi
}
function make_shared_dir {
  if [ $(getent group sf) ]; then
    DIR=$1
    sudo mkdir -p $DIR
    make_shared_file $DIR
  fi
}

echo "update semantic_forms Play! server when code has changed"
# SRC=$HOME/src/semantic_forms/scala/forms_play/
SRC=$PWD/forms_play
APP=semantic_forms_play
VERSION=2.1-SNAPSHOT
APPVERS=${APP}-$VERSION

INSTANCE=sandbox

# For single administrator
DEPLOY=~/deploy
# For multiple administrator
DEPLOY=/var/www/sf_deploy

SBT=sbt
MainXml=$SRC/../forms/src/main/scala/deductions/runtime/views/MainXml.scala

cd $SRC
git checkout -- $MainXml
git pull --verbose

DATE=`date`
sed -e "s/=timestamp=.*/=timestamp= $DATE/" $MainXml > /tmp/MainXml.scala
cp $MainXml /tmp/MainXml.orig.scala 
cp /tmp/MainXml.scala $MainXml
make_shared_file $MainXml

echo  which $SBT ; which $SBT
cd $SRC/..
echo Launching $SBT -J-Xmx2G 'project forms_play' 'dist'
# $SBT -J-Xmx2G 'project forms_play' 'dist'
$SBT -J-Xmx2G << EOF
  project forms_play
  dist
EOF
SBT_RETURN_CODE=$?
echo $SBT RETURN CODE: $SBT_RETURN_CODE
if test $SBT_RETURN_CODE -ne 0
then echo "Trouble in SBT!" ; exit
else
cp /tmp/MainXml.orig.scala $MainXml
echo "sofware recompiled!"
for f in */target
do
  make_shared_dir $f
done

mkdir -p $DEPLOY
cd $DEPLOY
kill `cat ${APPVERS}/RUNNING_PID`

# for keeping logs
rm -r ${APPVERS}_OLD
mv ${APPVERS} ${APPVERS}_OLD
make_shared_dir ${APPVERS}_OLD

unzip $SRC/target/universal/${APPVERS}.zip
make_shared_dir ${APPVERS}
cd ${APPVERS}
make_shared_dir ../TDB$INSTANCE
make_shared_dir ../TDB${INSTANCE}2
make_shared_dir ../TDB${INSTANCE}3
make_shared_dir ../LUCENE$INSTANCE

ln -s ../TDB$INSTANCE  TDB
ln -s ../TDB${INSTANCE}2 TDB2
ln -s ../TDB${INSTANCE}3 TDB3
ln -s ../LUCENE$INSTANCE LUCENE

PORT=9111
echo To start the server on port $PORT in directory $DEPLOY/$APPVERS , paste this:
echo cd  $DEPLOY/$APPVERS \; nohup bin/${APP} -J-Xmx100M -J-server -Dhttp.port=$PORT &
fi
# make_shared_file $DEPLOY/$APPVERS/nohup.out

