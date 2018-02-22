RDF_FILE=$1
LOAD_SERVICE=$2
GRAPH=$3
# default value for arg. 4, Content-Type
if [ -z "$3" ]; then
  echo 'Arguments: RDF_FILE LOAD_SERVICE GRAPH [MIME]'
  echo 'Content-Type [MIME] : application/ld+json application/rdf+xml text/turtle'
  exit
fi
if [ -z "$4" ]; then
    MIME=text/turtle
    echo "default value for MIME : $MIME"
else
    MIME=$4
    echo "value for MIME : $MIME"
fi

echo wget --output-file=FILE \
     --save-headers --post-file=$RDF_FILE \
     "$LOAD_SERVICE?graph=$GRAPH" \
     --header="Content-Type: $MIME"
wget --output-file=FILE \
     --save-headers --post-file=$RDF_FILE \
     "$LOAD_SERVICE?graph=$GRAPH" \
     --header="Content-Type: $MIME"
echo body-file=$RDF_FILE sent to server $LOAD_SERVICE in graph "<$GRAPH>"
echo result saved in FILE "FILE"
cat FILE
