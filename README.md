semantic forms
==============

Form generators leveraging semantic web standards RDF(S), OWL, SPARQL , JSON-LD , ...

This will be a building block for web application frameworks,
like Play! Framework, Ruby on Rails, Drupal, etc, but not SQL based.

These web application frameworks all provide tools to facilitate the creation of forms with storage in database, data validation, etc. We want this feature, but no SQL, SPARQL instead. And no SQL or business (domain) objects in Object Oriented language at the origin of design flow, but RDF and OWL vocabularies. More precisely, a form should be made up of RDF properties in the spirit of Fresnel (a Display Vocabulary for RDF).


I foresee 3 kinds of implementations, each of which can be deployed as web application, or a stand alone application:

* rules in N3 ; could leverage on Fresnel vocab' , see  [Forms generated from a resource or a class](http://svn.code.sf.net/p/eulergui/code/trunk/eulergui/html/documentation.html#L6973)

* in Scala generating HTML ; code here [./scala](./scala)

* pure JavaScript implementation ; see [./js](./js/README.md)


I developed in Scala with Swing semantic Web aware Forms; they are described here:
[Swing semantic Web aware Forms](http://svn.code.sf.net/p/eulergui/code/trunk/eulergui/html/documentation.html#Data1)

I have taken notes from my experience with Scala Swing N3 Forms and other; I added them here:

[Formulaires](http://htmlpreview.github.io/?https://github.com/jmvanel/semantic_forms/blob/master/doc/fr/formulaires.html)
<!-- alas github does not serve HTML as such : doc/fr/formulaires.html 
première version: http://jmvanel.free.fr/notes_fr/formulaires.html -->
 ( in french )

See also a broad review of Frameworks for semantic web applications:
[Frameworks for semantic web applications](http://svn.code.sf.net/p/eulergui/code/trunk/eulergui/html/semantic_based_apps_review.html)
including more details on this project goal in the introduction.

