#!/bin/bash

if [ -z "$1" ]; then
    echo 'rsparql.sh QUERY SERVER (full URL)'
    exit 1
fi
QUERY=$1

# default value for arg. 2
if [ -z "$2" ]; then
    SERVER=http://localhost:9000/sparql2
    echo "default value for server: $SERVER"
else
    SERVER=$2
    echo "server: $SERVER"
fi

echo "Processing query \"$QUERY\" on SPARQL server <$SERVER>"

wget    \
	--content-on-error \
	--method=POST \
	--body-data="query=$QUERY" \
	$SERVER

echo "SPARQL database <$SERVER> : processed query \"$QUERY\" ; RETURN_CODE: $?"
echo
