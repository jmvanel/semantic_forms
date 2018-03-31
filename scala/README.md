Scala implementations for HTML or other backends;
Play! framework application for generic semantic data manipulation and navigation

semantic\_forms is a framework for building all sorts of applications backed by a SPARQL database, and whose data model is specified by a sumple OWL ontology.
   
# Run from sources

```shell
sbt run
```

# Choice of applications and libraries

- generic web application in [forms\_play](forms_play) : usable both by semantic web aware users and non computer skilled users
- web services callable from JavaScript in browser for all features in [forms\_services](forms_services)
- an application for managing questions about entreprise practices, with simple login and user management, in [corporate\_risk](https://github.com/jmvanel/corporate_risk)
- an application for connecting Projects, Actors, Ideas, and Resources for [virtual assembly organization](http://www.virtual-assembly.org), in [github/assemblee-virtuelle/semforms](https://github.com/assemblee-virtuelle/semforms); similar to the generic application, but geared towards non computer skilled users
- a skills management application in [github/deductions/skills](https://github.com/deductions/skills)


# Features

* edit any semantic URI, with or without underlying ontology and/or form specification
* Creating an instance of given Class : a form is displayed; create a new unique URI every time
* RDF Cache (Jena TDB) : retrieve URI, download and store URI only if corresponding graph is empty and HTTP timestamp is younger
* URI and blank nodes are clickable, providing a blowser like navigation following forward and backward RDF statements 
* simple search
* I18N for labels and tooltips
* HTML pulldown menu with `<datalist>` for possible URI's inferred from rdfs:range, rdf:type, and rdfs:subclassOf 
* completion with dbPedia lookup
* a simple LDP server with POST and GET (partial implementation)
* a service for SPARQL CONSTRUCT queries
* a lookup service similar to dbPedia lookup

