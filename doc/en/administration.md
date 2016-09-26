# Running auxiliary programs

There are lots of auxiliary Scala programs, or Java ones from Jena, Lucene, etc.
There are 2 way of executing them:

- through SBT in the source directory
- from a shell in a deployed application

Through SBT, you use runMain, like this (from the shell):
```shell
sbt "runMain myPackage.myProgram arg1 arg2"
```

For example:
```shell
sbt "runMain tdb.tdbdump --loc=TDB" > dump.nq
```

*SBT trick*
If you need to run a program from the Unix shell with the classpath of semantic\_forms
```
CLASSPATH=`sbt "export runtime:fullClasspath"`
```
outputs the classpath as a colon-separated list perfect if you want to use it for invoking Scala.

From a shell in a deployed application, you must first set the claspath, by running:

    source scripts/setClasspath.sh

with does this:
```
for f in lib/*.jar
do
  JARS=$JARS:$f
done
```

Then you use the Java Virtual Machine the normal way, for example:

```shell
java -cp $JARS tdb.tdbdump --loc=TDB > dump.nq
```

## Database Administration

**CAUTION:**
The server must not be started, because Jena TDB does not allow access to the database on disk from 2 different processes.


## Loading RDF content

By running this, you also index with Lucene. The URL's can be file:// URL's.

    java -cp $JARS deductions.runtime.jena.RDFLoaderApp url1 url2 ...

   * downloads and stores URI content, in a graph named by its URI minus the # part,
   *  stores the timestamp from HTTP HEAD request;
   * loads also the direct owl:imports , but not recursively

**CAUTION:**
Do not load data in a named graph (argument --graph below) whose name is a relative URI, like "blabla", or "a/b/c" . Use an absolute URI like urn:data/myorganization or an HTTP URL (see URI spec. https://tools.ietf.org/html/rfc3986 ).

## Preloading RDF content

- Preloading common vocabularies, and preloading some pre-defined form specifications ( currently FOAF ) : in SBT shell, type:
```
    runMain deductions.runtime.sparql_cache.PopulateRDFCache
    // or, just some forms specs:
    runMain tdb.tdbloader --loc=TDB --graph=urn:form_specs ../forms/form_specs/foaf.form.ttl
```
Normally it is done just once, but in case of troubles (out of memory or server down) can be re-done (anyway because of SPARQL cache it goes quickly the second time).

PopulateRDFCache can run, with Lucene activated, with this memory setting:

    export SBT_OPTS="-Xmx4800M -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -Xss20M"

- Preloading a local file: in SBT shell type: for example:

```
    # load a FOAF profile from a local file :
    runMain tdb.tdbloader --loc=TDB --graph=http://jmvanel.free.fr/jmv.rdf#me /home/jmv/data/foaf/jmv.rdf
    # With Jena it is possible to directly load from Internet:
    runMain tdb.tdbloader --loc=TDB --graph=http://jmvanel.free.fr/jmv.rdf#me http://jmvanel.free.fr/jmv.rdf#me 
```
The typical pattern for data is to load in a graph named after the URI source, so that the cache feature works using the URI source timestamp.

CAUTION: do not load data or configuration into the un-named (default) graph. It would not be taken in account by the framework.

- dumping all database:

```
for f in lib/*.jar
do
  JARS=$JARS:$f
done
echo java -cp $JARS
java -cp $JARS tdb.tdbdump --loc=TDB > dump.nq
```
Or with sbt:

    sbt "runMain tdb.tdbdump --loc=TDB" > dump.nq

To re-load the database from N-Triples format (possibly delete the TDB directory before) :

    sbt "runMain tdb.tdbloader --loc=TDB dump.nq"

## Updating RDF content

Of course the simplest way to upload and update RDF content is to paste the URL in the top field "Disply", see the [User manual](https://github.com/jmvanel/semantic_forms/wiki/User_manual#navigating).

Run (with `java -cp $JARS` or `runMain` under SBT) deductions.runtime.utils.CommandLineApp for a commented list of Scala/Java Applications available in Semantic\_forms classpath.

The server must not be started while updading RDF content, because Jena TDB does not allow access to the database on disk from 2 different processes.

Take inspiration from these scripts in [forms\_play](https://github.com/jmvanel/semantic_forms/tree/master/scala/forms_play)

    dump.sh       graphload.sh	 graphremove.sh     populateRDFCache.sh  tdbsearch.sh
    graphdump.sh  graphnamedlist.sh  tdbquery.sh

For example, to update the I18N translations of the RDF vocabularies:

    GRAPH=urn:rdf-i18n
    echo "DROP GRAPH <$GRAPH>" > /tmp/delete_graph.rq    
    java -cp $JARS tdb.tdbupdate --loc=TDB --update=/tmp/delete_graph.rq
    java -cp $JARS deductions.runtime.sparql_cache.RDFI18NLoaderApp

To update the Common Form Specifications :

    GRAPH=urn:form_specs
    echo "DROP GRAPH <$GRAPH>" > /tmp/delete_graph.rq    
    java -cp $JARS tdb.tdbupdate --loc=TDB --update=/tmp/delete_graph.rq
    java -cp $JARS deductions.runtime.sparql_cache.FormSpecificationsLoader

To update everything (but better delete some named graph before, like above, or below) :

    java -cp $JARS deductions.runtime.sparql_cache.PopulateRDFCache

In the case when one wants to override some triples that are already loaded in graph G, and taking in account the language of the literals:

    java -cp $JARS deductions.runtime.jena.DataSourceManagerApp \
		file:///home/user/newTriples.tll 'http://jmvanel.free.fr/jmv.rdf#me'
    java -cp $JARS deductions.runtime.jena.DataSourceManagerApp \
		"https://raw.githubusercontent.com/assemblee-virtuelle/pair/master/form_labels.ttl" "rdf-i18n"
    # or:
    sbt "runMain deductions.runtime.jena.DataSourceManagerApp \
		https://raw.githubusercontent.com/assemblee-virtuelle/pair/master/form_labels.ttl rdf-i18n"

For each triple `?S ?P "val"@lang.` in newTriples.tll , this will remove the existing triple:  `?S ?P "old val"@lang.` , and add the new triple: `?S ?P "val"@lang.` 

To delete named graph related to common vocabularies, default form specifications, and IN18 translations, run this:

    java -cp $JARS deductions.runtime.jena.ResetRDFCache

## Semantize raw stuff

By "semantize" we mean transform into triples raw stuff like CSV, XML, JSON, SQL.
Actually there are several steps:

- obtain triples in the most direct way (sometimes called "direct mapping")
- change predicates to well-known predicates
- change values to well-known Linked Open Data (LOD) URI's (eg dbPedia or Wikidata URI's)
- modify the graph to add new URI's to have a model compliant with database normal forms and semantic web goods practices.

`semantic_forms` has a semantizer tool in command line: `deductions.runtime.jena.CSVImporterApp` .
 This application transforms CSV into a Turtle file; the args are:

 *  0 - URL or File of CSV,
 *  1 - base URL for the rows,
 *  2 - URL or File ( in Turtle ) for adding details to each row; for example it contains:
 *  		<any:ROW> a foaf:Person .
 *    which will add this triple to every row.
 
The features are like Any23, plus:
 * abbreviated Turtle terms with well-known prefixes (eg foaf:name) are understood as columns names 
 * abbreviated Turtle terms with well-known prefixes (eg dbpedia:Paris) are understood in cells 

Link on [other tools for semantization](http://svn.code.sf.net/p/eulergui/code/trunk/eulergui/html/documentation.html#L3234).

## SPARQL queries
There is a web page for SPARQL queries, and also a real SPARL endpoint at URL /sparql for CONSTRUCT queries (see below under Test about how to query with authentication).

There is an HTML page for entering queries, in the /tools page.
There are some example showing some queries that you can paste into your browser:

The service
<code>/sparql-ui?query=</code>
is for CONSTRUCT queries.

and the service
<code>/select-ui?query=</code>
is for SELECT queries.

## Populating with dbPedia (mirroring dbPedia)

There are 2 scripts, one for downloading, the other for populating the triple database.

    download-dbpedia.sh
    populate_with_dbpedia.sh

In `dbpedia-dbpedia.sh`you should set the LANG variable for the language you want for the rdfs:label and other properties. You can run the script several times with different LANG if several languages are needed.

    LANG=fr
    VERSION=2015-10

Beware: The script `populate_with_dbpedia_loop.sh` can take hours! You better have to start from a virgin database and use tdbloader2 instead of tdbloader, see 
https://jena.apache.org/documentation/tdb/commands.html#tdbloader
https://jena.apache.org/documentation/tdb/commands.html#tdbloader2


After the database is populated with dbPedia data, you should run this program to index with Lucene or SOLR the newly added text (see next paragraph).
[TextIndexerRDF.scala](https://github.com/jmvanel/semantic_forms/blob/master/scala/forms/src/main/scala/deductions/runtime/jena/lucene/TextIndexerRDF.scala)

`semantic_forms` does not download dbPedia URI's when it detects that dbPedia is loaded by `populate_with_dbpedia.sh` script
(implemented in [MirrorManagement.scala](https://github.com/jmvanel/semantic_forms/scala/forms/src/main/scala/deductions/runtime/sparql_cache/MirrorManagement.scala)).

`semantic_forms` with dbPedia mirroring has been tested with 2 Gb memory.
On disk this uses :

- 2,7Gb for Jena TDB (SPARQL database)
- 233Mb for Lucene (text index)


## TDB databases
- TDB/ : data: user edits and cached URL's from internet
- TDB2/ : timestamp for named graphs
- TDB3/ : user And Password association

# Text indexing with Lucene or SOLR

For details please look the Jena TDB documentation about text search with Lucene or SOLR in TDB :
https://jena.apache.org/documentation/query/text-query.html

If you run semantic\_forms from scratch, text search with Lucene is activated by default.
You have nothing to do, the necessary Lucene libraries are present, and the RDF content added is indexed in Lucene as soon as it is added.

The settings for text search with Lucene or SOLR in TDB here:
https://github.com/jmvanel/semantic\_forms/blob/master/scala/forms/src/main/scala/deductions/runtime/jena/LuceneIndex.scala
https://github.com/jmvanel/semantic\_forms/blob/master/scala/forms/src/main/scala/deductions/runtime/services/DefaultConfiguration.scala

If you deactivate `useTextQuery` in DefaultConfiguration.scala, the text search is done by a plain SPARLQL search, that considers input as a regular expression.

If the text indexing with Lucene or SOLR is activated *after* adding RDF data, you can run this program to index with Lucene or SOLR the newly added text:
[TextIndexerRDF.scala](https://github.com/jmvanel/semantic_forms/blob/master/scala/forms/src/main/scala/deductions/runtime/jena/lucene/TextIndexerRDF.scala)

    runMain deductions.runtime.jena.lucene.TextIndexerRDF

There is a script in the zip distribution : `scripts/index_lucene.sh`.

*NOTE:*
Since the association between TDB and Lucene is set by launching semantic\_forms, if you just add RDF content with tdb.tdbloader, you must afterwards index this content by running TextIndexerRDF.

To use Lucene with TDB , here is the code to configure (I don't use the Turtle config. as its vocab' is not documented in TTL ) :
https://github.com/jmvanel/semantic\_forms/blob/master/scala/forms/src/main/scala/deductions/runtime/jena/LuceneIndex.scala
This code is used in trait RDFStoreLocalJenaProvider in method `createDatabase(database_location: String)` .

Here is the program were an existing RDF content is indexed:
https://github.com/jmvanel/semantic\_forms/blob/master/scala/forms/src/main/scala/deductions/runtime/jena/lucene/TextIndexerRDF.scala

And here is where the text search is called:
https://github.com/jmvanel/semantic\_forms/blob/master/scala/forms/src/main/scala/deductions/runtime/services/StringSearchSPARQL.scala

[lucene core 4.6.1 doc.](https://lucene.apache.org/core/4_6_1/)

## Lucene commands

Index TDB with Lucene a posteriori:
```
runMain deductions.runtime.jena.lucene.TextIndexerRDF
INFO  14075 (2815 per second) properties indexed
```
Search the Lucene index with Lucene demo; see 
```
ln -s LUCENE  index
sbt "runMain org.apache.lucene.demo.SearchFiles"
```

