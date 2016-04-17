Scala implementations
---

Here is an HTML form generator in Scala;
it leverages on Banana-RDF.
( could be translated in JavaScript by Scala.js )


Currently it generates an Abstract Form (AF) from a resource,
then from AF it generates an XHTML form via the XML DSL of Scala.

The display looks like this: 
[example.form.foaf.html](http://htmlpreview.github.io/?https://github.com/jmvanel/semantic_forms/blob/master/scala/forms/example.form.foaf.html)

There is a [generic CRUD Web application based on Play! framework](../forms_play) .
The dependencies to Play! are kept to a minimum, and applications could be easily made using other Scala or Java web application frameworks, like Spray, Scalatra, or Lift.

