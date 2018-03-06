RDF_DIR=$1
LDP_SERVICE=$2

# default value for arg. 3, Content-Type
if [ -z "$2" ]; then
  echo 'Arguments: RDF_DIR LDP_SERVICE(full) [MIME]'
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
  GIVEN_PATH=$1
  GIVEN_DIR=$2
  file=`filename $GIVEN_PATH`
  GIVEN_DIR_PARENT=`dirname $GIVEN_DIR`
  GIVEN_PATH_PARENT=`dirname $GIVEN_PATH`
  REL_PATH=`echo $GIVEN_PATH_PARENT | sed 's|'$GIVEN_DIR_PARENT'/||'`  
  echo GIVEN_PATH $GIVEN_PATH, GIVEN_DIR $GIVEN_DIR, GIVEN_DIR_PARENT $GIVEN_DIR_PARENT, GIVEN_PATH_PARENT $GIVEN_PATH_PARENT, REL_PATH $REL_PATH
  RELATIVE_DIR=$REL_PATH
  echo wget --output-file=FILE \
     --save-headers --post-file=$GIVEN_PATH \
     $LDP_SERVICE/$RELATIVE_DIR \
     --header="Content-Type: $MIME" \
     --header="Slug: $file"
  wget --output-file=FILE \
     --save-headers --post-file=$GIVEN_PATH \
     $LDP_SERVICE/$RELATIVE_DIR \
     --header="Content-Type: $MIME" \
     --header="Slug: $file"

  echo file=$GIVEN_PATH sent to server $LDP_SERVICE
  echo result saved in FILE "FILE" :
  cat FILE
}

# for f in $RDF_DIR/*/*
# for f in $RDF_DIR/*
for f in `find $RDF_DIR -type f `
do
  put $f $RDF_DIR
done
