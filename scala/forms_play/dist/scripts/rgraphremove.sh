#!/bin/bash

if [ -z "$1" ]; then
  echo 'Arguments: GRAPH SERVER	( /update added to SERVER)'
  echo '           SERVER default: http://localhost:9000'
  exit
fi

if [ -z "$2" ]; then
    SERVER=http://localhost:9000
    echo "default value for SERVER : $SERVER/update"
else
    SERVER=$2
    echo "value for SERVER : $SERVER/update"
fi

PASSADMIN=gott-ueber-alles
GRAPH=$1

echo "Remove graph <$GRAPH> on SPARQL server $SERVER"
QUERY="DROP GRAPH <$GRAPH>"

wget --keep-session-cookies --save-cookies cookies.txt \
    --post-data "userid=admin&password=$PASSADMIN&confirmPassword=$PASSADMIN" \
    $SERVER/register
rm register

wget --load-cookies cookies.txt \
	--method=POST \
	--body-data="query=$QUERY" \
	$SERVER/update

echo "SPARQL database <$SERVER> : Removed (Enlevé) graph <$GRAPH> ; RETURN_CODE: $?"
echo result: ; cat update
rm update

