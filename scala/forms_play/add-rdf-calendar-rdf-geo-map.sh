# This script is an alternative to using git submodule

mkdir -p public/rdf-calendar/ ; cd public/rdf-calendar/
rm rdf-calendar.html rdfToArrayTimeStamps.js
GITHUB_DOWNLOAD=https://raw.githubusercontent.com/jmvanel/rdf-calendar/master/
wget $GITHUB_DOWNLOAD/rdf-calendar.html 
wget $GITHUB_DOWNLOAD/rdfToArrayTimeStamps.js

cd ../..
mkdir -p public/geo-map/ ; cd public/geo-map/
GITHUB_DOWNLOAD=https://raw.githubusercontent.com/jmvanel/rdf-geo-map/master/docs
for f in README.md geo-map-example-rdf.html rdf2array.js geo-map.html geo-map.js
do
  rm $f
  wget $GITHUB_DOWNLOAD/$f
done
# https://github.com/jmvanel/rdf-geo-map
