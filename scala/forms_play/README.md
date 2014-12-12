Play! framework implementations
---

# Introduction
Here is a web application with Play! framework around the [form generator](../forms/README.md).

With these features :

- 1. a SPARQL 1.1 server available - DONE
- 2. user enters an URI and form view appears with the data from Internet - DONE
- 2.1 user enters an URI and form view appears with the data from the SPARQL server (RDF cache) - DONE
- 2.2 show statistics about the current document : # of triples, # of properties, # of URI's
- 2.3 when document pointed by URI entered by user has no triple with that URI as subject, show a list of URI's in the document like the search results
- 3. URIs in the form can be clicked to display another form with the data from Internet - DONE
- 4. entering new triples for existing properties, as in DataGUI or as in Ontowiki: http://aksw.org/source/edit
- 5. introduce the RDF cache, - implemented - TODO unit test
- 6. creation of a new URI infering form from its class, as DomainApplication does - DONE
- 7. use HTTP HEAD to distinguish content types, and have different hyperlinks and styles for HTML, RDF, and image URL's - WIP
- 7.1 have icons to distinguish content types, to display near hyperlinks for HTML, RDF, image, etc URL's
- 8. data validation in JS or Scala.JS
- 9. creating or editing URI's : propose URI's in relation with rdfs:domain class value
- 10 simple vocab' to specify properties in form - DONE
- 11 button to remove a triple
- 12 Integrate non-blocking: Future, Iteratee Enumerator -WIP
- 13 Migration to Banana 0.7  - DONE
- 14 Migration to BigData(R)
- 15 ScalaJS migration 
- 16 Dashboard : # of triples, # of of resources; # of resources of each type
- 17 View SPARQL select & construct results
- 18 Search : search also in URI's
- 19 Add simple login 
- 20 Inferences for forms : eliminate archaic properties; implement owl union
- 21 write some JavaScript samples to call the different features
- 22 Add a button to edit the currently displayed URI
- 23 display the "reverse" triples ( called in-going links in BigData workbench )
- 24   (HTML) : add CSS classes for labels and values;
- 24.1 (HTML) : new HTML output with CSS rendering instead of explicit HTML table formatting
- 25   (HTML) : add component to enter a dbpedia URI ( use dbpedia lookup API )
- 26   (HTML) : add component to enter an ordered RDF list
- 27   (HTML) : add component to enter a choice (single or multiple) for owl:OneOf classes

..... etc etc ...

For now, the display looks like this, 
plus a textbox to enter a URL semantics, eg a FOAF profile or DBpedia URI : 
[example.form.foaf.html](http://htmlpreview.github.io/?https://github.com/jmvanel/semantic_forms/blob/master/scala/forms/example.form.foaf.html)

# How to run

Dependencies : Java 7 (Scala is not ready for Java 8), SBT or Typesafe Activator 

- build and run this project with [latest play activator from typesafe.com](http://typesafe.com/platform/getstarted) , and type in activator : ~run

To package to run on a server that has Java only: type in activator : dist

Then the archive is found here :
`target/universal/semantic_forms_play-1.0-SNAPSHOT.zip`
Donwnload this zip on the server, unzip and type:
```
nohup bin/semantic_forms_play -mem 50 &
```
If you want to change the log settings:
```
cp conf/log4j.properties myconf.properties
vi myconf.properties
nohup bin/semantic_forms_play -Dlog4j.configuration=myconf.properties -mem 50 &
```



# Preloading RDF content

- Preloading common vocabularies, and preloading some pre-defined form specifications ( currently FOAF ) : in activator shell type:
```
    run-main deductions.runtime.sparql_cache.PopulateRDFCache
```
- Preloading a local file: in activator shell type: for example:

```
    run-main tdb.tdbloader --loc=TDB --graph=http://jmvanel.free.fr/jmv.rdf#me
      /home/jmv/data/foaf/jmv.rdf
    # With Jena it is possible to directly load from Internet:
    run-main tdb.tdbloader --loc=TDB --graph=http://jmvanel.free.fr/jmv.rdf#me
       http://jmvanel.free.fr/jmv.rdf#me 
```

- dumping all database:

```
    activator run-main tdb.tdbdump   --loc=TDB > dump.nt
```

Tips:

- in case of troubles delete target/ directory
- create eclipse configurations with the "eclipse" command in sbt or activator:

```
    eclipse with-source=true
```

# Vocabulary for forms

At one point it will be useful to also introduce in place of FA an RDF vocabulary for forms, perhaps reusing Fresnel;
see a first attempt :
[vocabulary/forms.owl.ttl](../../vocabulary/forms.owl.ttl)

See (in french) :
[doc/fr/formulaires.html](http://htmlpreview.github.io/?https://github.com/jmvanel/semantic_forms/blob/master/doc/fr/formulaires.html)

