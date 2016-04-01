DATE=`date`
echo commit before updating running app. on line: DATE $DATE
MainXml=../forms/src/main/scala/deductions/runtime/html/MainXml.scala
sed -e "s/=timestamp=/$DATE/" $MainXml > /tmp/MainXml.scala
cp /tmp/MainXml.scala $MainXml
git add $MainXml
git commit "$*"
