semantic forms
==============

[![Join the chat at https://gitter.im/jmvanel/semantic_forms](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/jmvanel/semantic_forms?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Form generators leveraging semantic web standards RDF(S), OWL, SPARQL , JSON-LD , ...

Under GNU Public Licence version 3 [GPL v3](http://www.gnu.org/copyleft/gpl.html)

This is a building block for web application frameworks,
like Play! Framework, Ruby on Rails, Django, Drupal, etc, but not SQL based.

These web application frameworks all provide tools to facilitate the creation of forms connected with database storage, data validation, etc. We want this feature, but we want no SQL, we want SPARQL instead.
At the origin of design flow, we want RDF and OWL vocabularies.
No SQL or business (domain) objects in Object Oriented language.

More precisely, a form should be made up of RDF properties in the spirit of Fresnel (a Display Vocabulary for RDF).


 3 kinds of implementations, each of which can be deployed as web application, or a stand alone application:

* rules in N3 ; could leverage on Fresnel vocab' , see  [Forms generated from a resource or a class](http://svn.code.sf.net/p/eulergui/code/trunk/eulergui/html/documentation.html#L6973)
* in Scala generating HTML ; code here [./scala](./scala/README.md)
* pure JavaScript implementation ; see [./js](./js/README.md)


Here is the OWL vocabulary for ontolgy aware forms, currently used in the Scala application:
[vocabulary/forms.owl.ttl](../../vocabulary/forms.owl.ttl)

I

I developed in Scala with Swing semantic Web aware Forms; they are described here:
[Swing semantic Web aware Forms](http://svn.code.sf.net/p/eulergui/code/trunk/eulergui/html/documentation.html#Data1)

I have taken notes from my experience with Scala Swing N3 Forms and other; I added them here:

[Formulaires](https://htmlpreview.github.io/?https://github.com/jmvanel/semantic_forms/blob/master/doc/fr/formulaires.html)
<!-- alas github does not serve HTML as such : doc/fr/formulaires.html 
première version: http://jmvanel.free.fr/notes_fr/formulaires.html -->
 ( in french )

See also a broad review of Frameworks for semantic web applications:
[Frameworks for semantic web applications](http://svn.code.sf.net/p/eulergui/code/trunk/eulergui/html/semantic_based_apps_review.html)
including more details on this project goal in the introduction.

YourKit supports open source projects with its full-featured Java Profiler.
YourKit, LLC is the creator of <a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>
and <a href="https://www.yourkit.com/.net/profiler/">YourKit .NET Profiler</a>,
innovative and intelligent tools for profiling Java and .NET applications.
[[https://www.yourkit.com/images/yklogo.png|alt=yourkit]]
