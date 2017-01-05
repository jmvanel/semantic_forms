DATE=`date`

MainXml=../forms/src/main/scala/deductions/runtime/html/MainXml.scala
sed -e "s/=timestamp=/$DATE/" $MainXml > /tmp/MainXml.scala
cp $MainXml /tmp/MainXml.orig.scala
cp /tmp/MainXml.scala $MainXml

echo commit before updating running app. on line: DATE $DATE
git add $MainXml
git commit "$*"

cp /tmp/MainXml.orig.scala $MainXml

