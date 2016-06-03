#!/bin:bash

LANG=fr
VERSION=2015-04
DBPEDIA=http://downloads.dbpedia.org/$VERSION/core-i18n
EXT=ttl.bz2

mkdir --parents ~/data/dbpedia.org/$VERSION
cd ~/data/dbpedia.org/$VERSION


# http://downloads.dbpedia.org/2015-04/dbpedia_2015-04.owl.bz2
FILE=dbpedia_${VERSION}.owl.bz2
OWL=$DBPEDIA/../$FILE
wget $OWL ; bunzip2 $FILE

# http://downloads.dbpedia.org/2015-04/core-i18n/fr/instance-types-en-uris_fr.ttl.bz2
# http://downloads.dbpedia.org/2015-04/core-i18n/fr/mappingbased-properties-en-uris_fr.ttl.bz2
# http://downloads.dbpedia.org/2015-04/core-i18n/fr/infobox-properties-en-uris_fr.ttl.bz2

function get_one_url {
  NAME=$1
  FILE=${NAME}-en-uris_${LANG}.$EXT
  URL=$DBPEDIA/$LANG/$FILE
  wget $URL
  bunzip2 $FILE &
  echo DONE $FILE from $URL
}

get_one_url 'instance-types'
get_one_url 'mappingbased-properties'
get_one_url 'labels'
get_one_url 'short-abstracts'
get_one_url 'infobox-properties'
get_one_url 'article-categories'
get_one_url 'category-labels'
get_one_url 'skos-categories'

