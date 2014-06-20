semantic forms
==============

Form generators leveraging semantic web standards RDF(S), OWL, SPARQL , JSON-LD , ...

This will be a building block for web application frameworks,
like Play! Framework, Ruby on Rails, Drupal, etc, but not SQL based.

These web application frameworks all provide tools to facilitate the creation of forms with storage in database, data validation, etc. We want this feature, but no SQL, SPARQL instead. And no SQL or business (domain) objects in Object Oriented language at the origin of design flow, but RDF and OWL vocabularies. More precisely, a form should be made up of RDF properties in the spirit of Fresnel (a Display Vocabulary for RDF).


I foresee 3 kinds of implementations:

* rules in N3 ; could leverage on Fresnel vocab' 

* in Scala generating HTML ; code here
[./scala](./scala)

* pure JavaScript implementation


I developed in Scala with Swing semantic Web aware Forms; they are described here:
[Swing semantic Web aware Forms](http://svn.code.sf.net/p/eulergui/code/trunk/eulergui/html/documentation.html#Data1)

I have taken notes from my experience with Scala Swing N3 Forms and other; I added them here:
ttp://jmvanel.free.fr/notes_fr/formulaires.html#L361
[Formulaires](doc/fr/formulaires.html)
 ( in french )

See also a broad review of Frameworks for semantic web applications:
[Frameworks for semantic web applications](http://svn.code.sf.net/p/eulergui/code/trunk/eulergui/html/semantic_based_apps_review.html)
including more details on this project goal in the introduction.

