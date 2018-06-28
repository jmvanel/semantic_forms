wget --method=POST \
     --body-data="query=SELECT * WHERE {?S ?P ?O} LIMIT 10" \
     --header='Accept:application/sparql-results+xml' \
     http://localhost:9000/sparql2

