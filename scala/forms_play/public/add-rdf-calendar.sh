# This script is an alternative to using git submodule

mkdir -p rdf-calendar/
cd rdf-calendar/
rm rdf-calendar.html rdfToArrayTimeStamps.js
GITHUB_DOWNLOAD=https://raw.githubusercontent.com/jmvanel/rdf-calendar/master/
wget $GITHUB_DOWNLOAD/rdf-calendar.html 
wget $GITHUB_DOWNLOAD/rdfToArrayTimeStamps.js

