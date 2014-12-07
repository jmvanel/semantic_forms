Play! framework implementations
---

# Introduction
Here is a web application with Play! framework around the [form generator](../forms/README.md).

With these features :

- 1. a SPARQL 1.1 server available - DONE
- 2. user enters an URI and form view appears with the data from Internet - DONE
- 2.1 user enters an URI and form view appears with the data from the SPARQL server (RDF cache) - DONE
- 3. URIs in the form can be clicked to display another form with the data from Internet - DONE
- 4. entering new triples for existing properties, as in DataGUI or as in Ontowiki: http://aksw.org/source/edit
- 5. introduce the RDF cache, - implemented - TODO test
- 6. creation of a new URI infering form from its class, as DomainApplication does - DONE
- 7. use HTTP HEAD to distinguish content types, and have different hyperlinks and styles for HTML, RDF, and image URL's
- 8. data validation in JS or Scala.JS
- 9. creating or editing URI's : propose URI's in relation with rdfs:domain class value
- 10 simple vocab' to specify properties in form - DONE
- 11 button to remove a triple

Integrate non-blocking: Future, Iteratee Enumerator
Migration to Banana 0.7 
Migration to BigData(R)
ScalaJS migration 
Dashboard : # of triples, # of of resources; # of resources of each type
View SPARQL select & construct results
Search : also in URI's
Add simple login 
Inferences for forms : eliminate archaic properties; implement owl union
..... etc etc ...

For now, the display looks like this, 
plus a textbox to enter a URL semantics, eg a FOAF profile or DBpedia URI : 
[example.form.foaf.html](http://htmlpreview.github.io/?https://github.com/jmvanel/semantic_forms/blob/master/scala/forms/example.form.foaf.html)

# How to run

- build and run this project with [latest play activator from typesafe.com](http://typesafe.com/platform/getstarted) , and type in activator : ~run

Preloading RDF content

- Preloading common vocabularies: in activator shell type:

	run-main deductions.runtime.sparql_cache.PopulateRDFCache
- Preloading a local file: in activator shell type: for example:

	run-main tdb.tdbloader --loc=TDB --graph=http://jmvanel.free.fr/jmv.rdf#me /home/jmv/data/foaf/jmv.rdf

	// With Jena it is possible to directly load from Internet:
	run-main tdb.tdbloader --loc=TDB --graph=http://jmvanel.free.fr/jmv.rdf#me http://jmvanel.free.fr/jmv.rdf#me 

- dumping all database:

	run-main tdb.tdbdump   --loc=TDB > dump.nt

Tips:

- in case of troubles delete target/ directory
- create eclipse configurations with the "eclipse" command in sbt and activator

# Vocabulary for forms
At one point it will be useful to also introduce in place of FA an RDF vocabulary for forms, perhaps reusing Fresnel;
see a first attempt :
[vocabulary/forms.owl.ttl](../../vocabulary/forms.owl.ttl)

See (in french) :
[doc/fr/formulaires.html](http://htmlpreview.github.io/?https://github.com/jmvanel/semantic_forms/blob/master/doc/fr/formulaires.html)

