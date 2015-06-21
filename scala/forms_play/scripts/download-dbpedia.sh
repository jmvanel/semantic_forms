mkdir --parents ~/data/dbpedia.org_2014/
cd ~/data/dbpedia.org_2014/

LANG=fr
DBPEDIA=http://data.dws.informatik.uni-mannheim.de/dbpedia/2014
EXT=ttl.bz2

function get_one_url {
  NAME=$1
  FILE=${NAME}_en_uris_fr.$EXT
  URL=$DBPEDIA/$LANG/$FILE
  wget $URL
  bunzip2 $FILE &
}

# http://data.dws.informatik.uni-mannheim.de/dbpedia/2014/dbpedia_2014.owl.bz2
FILE=dbpedia_2014.owl.bz2
OWL=$DBPEDIA/$FILE
wget $OWL ; bunzip2 $FILE

get_one_url( 'mappingbased_properties' )
get_one_url( 'labels' )
get_one_url( 'short_abstracts' )
get_one_url( 'article_categories' )
get_one_url( 'category_labels' )
get_one_url( 'skos_categories' )

