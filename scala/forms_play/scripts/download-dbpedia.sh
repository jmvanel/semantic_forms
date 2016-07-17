#!/bin:bash

LANG=fr
DBPEDIA_VERSION=2015-10
DBPEDIA_DOWNLOAD=http://downloads.dbpedia.org/$DBPEDIA_VERSION/core-i18n
EXT=ttl.bz2

mkdir --parents ~/data/dbpedia.org/$DBPEDIA_VERSION
cd ~/data/dbpedia.org/$DBPEDIA_VERSION


# http://downloads.dbpedia.org/2015-10/dbpedia_2015-10.nt
FILE=dbpedia_${DBPEDIA_VERSION}.nt
OWL=$DBPEDIA/../$FILE
wget $OWL

# on site 20106-07
# with _en_uris_
# http://downloads.dbpedia.org/2015-10/core-i18n/fr/instance_types_en_uris_fr.ttl.bz2
# http://downloads.dbpedia.org/2015-10/core-i18n/fr/mappingbased_objects_en_uris_fr.ttl.bz2
# http://downloads.dbpedia.org/2015-10/core-i18n/fr/mappingbased_literals_en_uris_fr.ttl.bz2

# before, in 2015
# http://downloads.dbpedia.org/2015-10/core-i18n/fr/instance-types-en-uris_fr.ttl.bz2
# http://downloads.dbpedia.org/2015-10/core-i18n/fr/infobox-properties-en-uris_fr.ttl.bz2

function get_one_url_en_uris {
  NAME=$1
  FILE=${NAME}_en_uris_${LANG}.$EXT
  URL=$DBPEDIA_DOWNLOAD/$LANG/$FILE
  wget $URL
  if [ $? -eq 0 ] ; then echo DONE $FILE from $URL ; else DOWNLOAD FAILED! ; fi
  bunzip2 $FILE &
}

get_one_url_en_uris 'article_categories'
get_one_url_en_uris 'category_labels'
get_one_url_en_uris 'instance_types'
get_one_url_en_uris 'labels'
get_one_url_en_uris 'mappingbased_literals'
get_one_url_en_uris 'mappingbased_objects'
get_one_url_en_uris 'short_abstracts'
get_one_url_en_uris 'skos_categories'
# "less clean":	get_one_url 'infobox-properties'

