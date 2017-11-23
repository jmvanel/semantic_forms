RDF_FILE=$1
LOAD_SERVICE=$2
GRAPH=$3

# --method=POST
echo wget --output-file=FILE \
     --save-headers --post-file=$RDF_FILE \
     "$LOAD_SERVICE?graph=$GRAPH" \
     --header='Content-Type: text/turtle'
wget --output-file=FILE \
     --save-headers --post-file=$RDF_FILE \
     "$LOAD_SERVICE?graph=$GRAPH" \
     --header='Content-Type: text/turtle'
echo body-file=$RDF_FILE sent to server $LOAD_SERVICE in graph "<$GRAPH>"
echo result saved in FILE "FILE"
