Play! framework implementations
---

# Introduction
Here is a web application with Play! framework around the [form generator](../forms/README.md).

With these features to add one after the other:

- 1. a SPARQL 1.1 server available (BigData (R)). - DONE
- 2. user enters an URI and form view appears with the data from Internet - DONE
- 2.1 user enters an URI and form view appears with the data from the SPARQL server
- 3. URIs in the form can be clicked to display another form with the data from Internet - DONE
- 4. entering new triplets, as in DataGUI or as in Ontowiki: http://aksw.org/source/edit
- 5. introduce the RDF cache, creation of a new URI in its class as DomainApplication ..... etc etc ...

For now, the display looks like this, 
plus a textbox to enter a URL semantics, eg a FOAF profile or DBpedia URI : 
[example.form.foaf.html](http://htmlpreview.github.io/?https://github.com/jmvanel/semantic_forms/blob/master/scala/forms/example.form.foaf.html)

# How to run

- build with sbt project ../forms
- build and run with latest play activator this project : type run

Tips:

- in case of troubles delete target/ directory
- create eclipse configurations with the "eclipse" command in sbt and activator

# Vocabulary for forms
At one point it will be useful to also introduce in place of FA an RDF vocabulary for forms, perhaps reusing Fresnel;
see a first attempt :
[vocabulary/forms.owl.ttl](../../vocabulary/forms.owl.ttl)

See (in french) :
[doc/fr/formulaires.html](http://htmlpreview.github.io/?http://github.com/jmvanel/semantic_forms/blob/master/doc/fr/formulaires.html)

