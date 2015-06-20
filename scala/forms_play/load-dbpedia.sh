
EXT=ttl
DATA=~/data/dbpedia.org_2014

source graphload.sh

graphload $DATA/'mappingbased_properties'_en_uris_fr.$EXT 'dbpedia'
graphload $DATA/'labels'_en_uris_fr.$EXT 'dbpedia'
graphload $DATA/'short_abstracts'_en_uris_fr.$EXT 'dbpedia'
graphload $DATA/'article_categories'_en_uris_fr.$EXT 'dbpedia'
graphload $DATA/'category_labels'_en_uris_fr.$EXT 'dbpedia'
graphload $DATA/'skos_categories'_en_uris_fr.$EXT 'dbpedia'
graphload $DATA/'dbpedia_2014.owl' 'dbpedia'
