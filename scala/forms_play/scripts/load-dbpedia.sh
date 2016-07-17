
EXT=ttl
DBPEDIA_VERSION=2015-10
DBPEDIA_DIR=~/data/dbpedia.org/$DBPEDIA_VERSION
DATA=$DBPEDIA_DIR

source graphload.sh

graphload $DATA/'article_categories'_en_uris_fr.$EXT 'dbpedia'
graphload $DATA/'category_labels'_en_uris_fr.$EXT 'dbpedia'
graphload $DATA/'instance_types'_en_uris_fr.$EXT 'dbpedia'
graphload $DATA/'labels'_en_uris_fr.$EXT 'dbpedia'
graphload $DATA/'mappingbased_literals'_en_uris_fr.$EXT 'dbpedia'
graphload $DATA/'mappingbased_objects'_en_uris_fr.$EXT 'dbpedia'
graphload $DATA/'short_abstracts'_en_uris_fr.$EXT 'dbpedia'
graphload $DATA/'skos_categories'_en_uris_fr.$EXT 'dbpedia'
graphload $DATA/'dbpedia_2015-10.nt' 'dbpedia'
