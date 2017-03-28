#!/bin/bash

PASSADMIN=gott-ueber-alles

QUERY=$1
SERVER=http://localhost:9000
SERVER=$2
# TODO default value for arg. 2

echo "Remove graph <$GRAPH> on SPARQL server $SERVER"

wget --keep-session-cookies --save-cookies cookies.txt \
    --post-data "userid=admin&password=$PASSADMIN&confirmPassword=$PASSADMIN" \
    $SERVER/register
rm register

wget --save-headers \
	--content-on-error \
	--load-cookies cookies.txt \
	--method=POST \
	--body-data="query=$QUERY" \
	$SERVER/update

echo "SPARQL database <$SERVER> : Removed (Enlevé) graph <$GRAPH> ; RETURN_CODE: $?"
echo result: ; cat update
rm update
echo
