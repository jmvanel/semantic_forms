I am pleased to announce `Semantic_forms` release 1.4 .

`Semantic_forms` is a generic application for navigation of [LOD](https://fr.wikipedia.org/wiki/Linked_open_data) ( **Linked Open Data** ).
Moreover, it allows for annotation, editing of structured data. It is a software framework to build **business applications**, which is forms centric (input or read only). This is a step towards Semantic Information System, which could replace ERP ([Enterprise Resource Planning](https://en.wikipedia.org/wiki/Enterprise_resource_planning)) .

All data and all data models (ontologies) are based on the recommendations of the **W3C Semantic Web**:
[ `Wikipedia / semantic_web`](https://en.wikipedia.org/wiki/Semantic_Web).
Specifically, a form is inferred on the fly from an instance URI, or from a class URI.
Or, a form can be defined as an RDF list of properties.

`Semantic_forms` leverages on Jena TDB through API, and uses Lucene and SOLR integration in Jena.
It is developed in Scala. The generic web application uses Play! framework, but there are little Play! dependencies.
Thanks to Play! framework, the generic web application can be deployed solely from the zipped distribution.
 
Thanks to the /form API, it is possible to build a web application completely in Javascript and HTML.
Using the dbpedia lookup service, it is easy in the UI to link to dbPedia URI's.

There is partial support for LDP protocol.
The SPARQL database is used as a cache for downloaded RDF URL's. When dbPedia is mirrored, the cache is not used for dbPedia URI's .
  
Distribution:
https://github.com/jmvanel/semantic_forms/releases

Installation:
https://github.com/jmvanel/semantic_forms/blob/master/doc/en/install.md#installation-of-the-semantic_forms-generic-application

User manual:
https://github.com/jmvanel/semantic_forms/wiki/User_manual

Application developer manual:
https://github.com/jmvanel/semantic_forms/wiki/Application-development-manual

Administration manual:
https://github.com/jmvanel/semantic_forms/blob/master/doc/en/administration.md


