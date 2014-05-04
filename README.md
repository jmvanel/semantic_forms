semantic_forms
==============

Form generators leveraging semantic web standards (RDF(S), OWL, SPARQL , ...

This will be a building block for web application frameworks,
like Play! Framework, Ruby on Rails, Drupal, etc, but not SQL based.

These web application frameworks all provide tools to facilitate the creation of forms with storage in database, data validation, etc. We want this, but no SQL, SPARQL instead. And no business (domain) objects in Object Oriented language at the origin of design flow, but RDF and OWL vocabularies. More precisely, a form should be made up of RDF properties in the spirit of Fresnel (a Display Vocabulary for RDF).

See more details on the project goal in introduction of :
http://svn.code.sf.net/p/eulergui/code/trunk/eulergui/html/semantic_based_apps_review.html
plus a review of existing stuff.

I foresee 3 implementations:

- rules in N3 ; could leverage on Fresnel vocab' (see N3 rules for Java Swing : http://svn.code.sf.net/p/eulergui/code/trunk/eulergui/html/documentation.html#L6973 )
- in Scala generating HTML ; already some code here: http://svn.code.sf.net/p/eulergui/code/trunk/eulergui/src/test/scala/deductions/runtime/html/Form2HTMLTest.scala ( could be translated in JavaScript by Scala.js )
- pure JavaScript implementation, like http://viejs.org/ or http://www.hydra-cg.com/


My current thoughts about web RDF forms :

1) study what Ruby on rails , Play! fw and others do for SQL or object oriented backed forms
2) finish what I have begun with Scala HTML form generator : 
 src/main/scala/deductions/runtime/abstract_syntax/FormModule.scala
 src/main/scala/deductions/runtime/abstract_syntax/FormSyntaxFactory.scala
 src/main/scala/deductions/runtime/html/Form2HTML.scala
in http://jmvanel@svn.code.sf.net/p/eulergui/code
3) redo it with N3 rules like I did for Swing generator
4) the form abstract syntax could leverage on some of Fresnel vocab', but adding some specific properties like mandatory field.

I have taken paper notes from my experience with Scala Swing N3Forms ; I'll type them .
