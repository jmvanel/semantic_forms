RDF_DIR=$1
LDP_SERVICE=$2

# default value for arg. 3, Content-Type
if [ -z "$2" ]; then
  echo 'Arguments: RDF_DIR LDP_SERVICE [MIME]'
  echo 'Content-Type [MIME] : application/ld+json application/rdf+xml text/turtle'
  exit
fi
if [ -z "$3" ]; then
    MIME=text/turtle
    echo "default value for MIME : $MIME"
else
    MIME=$3
    echo "value for MIME : $MIME"
fi

# LDP PUT
function put {
  RELATIVE_PATH=$1
  file=`filename $RELATIVE_PATH`
  RELATIVE_DIR=`dirname $RELATIVE_PATH`
  echo wget --output-file=FILE \
     --save-headers --post-file=$RDF_DIR/RELATIVE_DIR \
     "$LDP_SERVICE \
     --header="Content-Type: $MIME" \
     --header="Slug: $file"
  wget --output-file=FILE \
     --save-headers --post-file=$RDF_DIR/RELATIVE_DIR \
     "$LDP_SERVICE \
     --header="Content-Type: $MIME" \
     --header="Slug: $file"

  echo body-file=$RDF_DIR/RELATIVE_PATH sent to server $LDP_SERVICE 
  echo result saved in FILE "FILE"
  cat FILE
}

# for f in $RDF_DIR/*/*
for f in $RDF_DIR/*
do
  put $f
done
