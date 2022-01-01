#!/bin/bash

PASSADMIN=gott-ueber-alles

if [ -z "$1" ]; then
    echo 'rupdate.sh QUERY (file) SERVER (without /update) - default SERVER=http://localhost:9000'
    exit 1
fi
QUERY_FILE=$1
QUERY="`cat $QUERY_FILE`"
# default value for arg. 2
if [ -z "$2" ]; then
    SERVER=http://localhost:9000
    echo "default value for server: $SERVER"
else
    SERVER=$2
fi
echo "server prefix: $SERVER"


echo "Processing update \"$QUERY_FILE\" on SPARQL server <$SERVER>"

wget --keep-session-cookies --save-cookies cookies.txt \
    --post-data "userid=admin&password=$PASSADMIN&confirmPassword=$PASSADMIN" \
    $SERVER/register
rm register

urlencode() {
     # urlencode <string>
     old_lc_collate=$LC_COLLATE
     LC_COLLATE=C

     local length="${#1}"
     for (( i = 0; i < length; i++ )); do
         local c="${1:i:1}"
         case $c in
             [a-zA-Z0-9.~_-]) printf "$c" ;;
             *) printf '%%%02X' "'$c" ;;
         esac
     done

     LC_COLLATE=$old_lc_collate
     echo
}

QUERY_ENCODED=`urlencode "$QUERY"`
echo QUERY is URL ENCODED for HTTP method=POST

wget --save-headers \
	--content-on-error \
	--load-cookies cookies.txt \
	--method=POST \
	--body-data="query=$QUERY_ENCODED" \
	$SERVER/update

echo "SPARQL database <$SERVER> : processed update \"$QUERY_FILE\" ; RETURN_CODE: $?"
echo result: ; cat update
rm update
echo
