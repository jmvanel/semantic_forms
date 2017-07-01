#!/bin/bash

PASSADMIN=gott-ueber-alles

if [ -z "$1" ]; then
    echo rupdate.sh QUERY SERVER
    exit 1
fi
QUERY=$1
# default value for arg. 2
if [ -z "$2" ]; then
    SERVER=http://localhost:9000
    echo "default value for server: $SERVER"
else
    SERVER=$2
    echo "server: $SERVER"
fi


echo "Processing update \"$QUERY\" on SPARQL server <$SERVER>"

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

echo "SPARQL database <$SERVER> : processed update \"$QUERY\" ; RETURN_CODE: $?"
echo result: ; cat update
rm update
echo
