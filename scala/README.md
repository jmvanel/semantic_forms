Scala implementations for HTML or other backends
Play! framework application for generic semantic data manipulation and navigation

semantic\_forms is a framework for building all sorts of applications backed by a SPARQL database, and whose data model is specified by a sumple OWL ontology.

semantic\_forms is similar to dbPedia.org view, and in addition any RDF URL on the web is viewable, and forms generated on the fly allow to add triples to external URI's.
NOTE that everyone has complete control on her/his input, it's not like a wiki, although every user can add anything anywhere, conforming to LOD principles .

# Run from sources

```shell
sbt run
```
Details and links [ here ](forms_play/README.md) .

# Features

* edit any semantic URI, with or without underlying ontology and/or form specification
* Creating an instance of given Class : a form is displayed; create a new unique URI every time
* simple user management: a FOAF account with a password, that can be associated to a FOAF Person
* RDF Cache (Jena TDB) : retrieve URI, download and store URI only if corresponding graph is empty and HTTP timestamp is younger
* URI and blank nodes are clickable, providing a browser like navigation following forward RDF statements
* other views; backward links, history, named graphs, table, maps, paths of length 2, forms from SPARQL query
* simple search (LUCENE based)
* I18N for labels and tooltips (en, fr, de, es)
* HTML pulldown menu for possible URI's inferred from rdfs:range, rdf:type, and rdfs:subclassOf (using internal /lookup service )
* completion with dbPedia lookup
* a simple LDP server with POST and GET (partial implementation)
* compliant SPARQL service; simple HTML pages for queries (yasgui also usable)
* SPARQL update service
* lookup service similar to dbPedia lookup

