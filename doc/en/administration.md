<!--
gh-md-toc --insert administration.md
pandoc --standalone administration.md > administration.html -->

Application and database administration
=======================================

<!--ts-->
   * [Application and database administration](#application-and-database-administration)
   * [Introduction](#introduction)
   * [Running auxiliary programs](#running-auxiliary-programs)
      * [Running with SBT](#running-with-sbt)
      * [Running with the Unix shell](#running-with-the-unix-shell)
   * [Database Administration](#database-administration)
      * [TDB databases](#tdb-databases)
      * [Loading RDF content](#loading-rdf-content)
      * [Preloading RDF content](#preloading-rdf-content)
      * [Updating and loading RDF content](#updating-and-loading-rdf-content)
         * [Jena commands](#jena-commands)
         * [Remote shell commands](#remote-shell-commands)
         * [Administrative SPARQL](#administrative-sparql)
         * [Misc.](#misc)
      * [Dump and reload](#dump-and-reload)
         * [Example of a shell session to reload a dump](#example-of-a-shell-session-to-reload-a-dump)
      * [Mirror SPARQL site into SF semantic cache](#mirror-sparql-site-into-sf-semantic-cache)
   * [Semantize raw stuff](#semantize-raw-stuff)
   * [SPARQL queries](#sparql-queries)
   * [Populating with dbPedia (mirroring dbPedia)](#populating-with-dbpedia-mirroring-dbpedia)
   * [Text indexing with Lucene or SOLR](#text-indexing-with-lucene-or-solr)
      * [Lucene commands](#lucene-commands)

<!-- Added by: jmv, at: 2018-04-10T13:19+02:00 -->

<!--te-->

# Introduction

Typically you have downloaded the zipped application (or build it from sources).

Then run the generic server like this:

```shell
PORT=9000
bin/semantic_forms_play -J-Xmx2000M -J-server -Dhttp.port=$PORT
```

For more details on running the zipped application, see [installation](install.md).


# Running auxiliary programs

There are lots of auxiliary Scala programs, or Java ones from Jena, Lucene, etc.
There are 2 way of executing them:

- through SBT in the source directory
- from a shell in a deployed application

## Running with SBT
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
JARS=`sbt "export runtime:fullClasspath"`
```
outputs the classpath as a colon-separated list perfect if you want to use it for invoking Scala.

## Running with the Unix shell
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

# Database Administration

**CAUTION:**
When running command line scripts, the server must not be started, because Jena TDB does not allow access to the database on disk from 2 different processes.

Be sure to read previous paragraph [Running auxiliary programs](#running-auxiliary-programs) before running the commands below.

`semantic_forms` is one of those programs that are mainly driven by what is inside the database, which contains:

- ontologies (data structure definition)
- form specifications
- internationalization labels (I18N) for ontologies
- some additions to existing ontologies, that are not always accurate enough for SF, see [additions\_to\_vocabs.ttl](../../scala/forms/form_specs/additions_to_vocabs.ttl)

So, according to her needs, the application manager can:

- preload RDF content with a JVM (Scala) script, see [preloading RDF content](#preloading-rdf-content)
- run with the "naked" database (and then use the links in the data pages `/display` to download ontologies and form specifications as needed)
- devise a personalized script, either using the Scala API, or the HTTP API

or a combination of the above. It is generally not a problem to run the script for preloading RDF content; there is a script to undo all or one of the 4 categories of configuration data.

## TDB databases
- TDB/ : data: user edits and cached URL's from internet
- TDB2/ : timestamp for named graphs
- TDB3/ : user And Password association

## Loading RDF content

By running this, you also index with Lucene. The URL's can be file:// URL's.

    java -cp $JARS deductions.runtime.sparql_cache.apps.RDFLoaderApp url1 url2 ...

   * downloads and stores URI content, in a graph named by its URI minus the # part,
   * stores the timestamp from HTTP HEAD request;
   * loads also the direct owl:imports , but not recursively

Works much like the /load service, see
https://www.w3.org/TR/2013/REC-sparql11-http-rdf-update-20130321/#http-post
and
https://github.com/jmvanel/semantic_forms/wiki/Application-development-manual#sparql-11-graph-store-http-protocol


## Preloading RDF content

- Preloading common vocabularies, and preloading some pre-defined form specifications ( currently FOAF ) : in SBT shell, type:
```
    runMain deductions.runtime.sparql_cache.apps.PopulateRDFCache
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

**CAUTION:**
Do not load data in a named graph (argument --graph above) whose name is a relative URI, like "blabla", or "a/b/c" . Use an absolute URI like urn:data/myorganization or an HTTP URL (see URI spec. https://tools.ietf.org/html/rfc3986 ).


The typical pattern for data in SF is to load in a graph named after the URI source, so that the cache feature works using the URI source timestamp.
This is the case for the ontologies. But all the from specifications loaded by PopulateRDFCache are in the graph `urn:form_specs` .
The I18N stuff is managed in the file [translations\_list.ttl](https://github.com/jmvanel/rdf-i18n/blob/master/translations_list.ttl) in a sister github project.
For example, the FOAF I18N stuff in https://github.com/jmvanel/rdf-i18n/tree/master/foaf is loaded in their respective HTPP github paths and corresponding graph URI
https://rawgit.com/jmvanel/rdf-i18n/master/foaf/foaf.fr.ttl .

**CAUTION** do not load data or configuration into the un-named (default) graph. It would not be taken in account by the framework.


## Updating and loading RDF content

Of course the simplest way to upload and update RDF content is to paste the URL in the top field "Disply", see the [User manual](https://github.com/jmvanel/semantic_forms/wiki/User_manual#navigating). But this works for RDF data that are available via HTTP.
If you have RDF content as a local file, you can load it via the `/load` service; see below "Remote shell commands".

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

To delete named graph related to common vocabularies, default form specifications, and IN18 translations, run this:

    java -cp $JARS deductions.runtime.jena.ResetRDFCache

To delete named graph for just one of the above:

    scala -cp $JARS
      deductions.runtime.jena.ResetRDFCacheApp.resetCommonVocabularies()
      deductions.runtime.jena.ResetRDFCacheApp.resetCommonFormSpecifications()
      deductions.runtime.jena.ResetRDFCacheApp.resetRDFI18NTranslations()

### Jena commands 

SPARQL query in TDB:

    java -cp $JARS tdb.tdbquery --loc=$HOME/src/semantic_forms/scala/forms_play/TDB --query /tmp/test.rq

Converting RDF file in any syntax in JSON-LD. This is particularly useful, as "classic" command line tools like CWM and rapper cannot do that.

    java -cp $JARS riotcmd.riot --out=json-ld ~/data/sioc1.rdf 1> ~/data/sioc1.jsonld

Moreover, the error meages are more accurate than  CWM and rapper.

### Remote shell commands

These commands connect to the database, with admin account, by SPARQL Update protocol:

```shell
# remove a named graph
scripts/rgraphremove.sh
# any SPARQL Update command
scripts/rupdate.sh
```

If you have RDF content as a local file, you can load it via the `/load` service (compliant with SPARQL 1.1 Graph Store HTTP Protocol ). There is convenient script for that:

```shell
scripts/load_wget.sh my.data.ttl semantic_forms.cc:9111/load my:data
```
The arguments are :
```
RDF_FILE
LOAD_SERVICE
GRAPH
MIME
```
MIME is Turtle by default.

### Administrative SPARQL

Some typical SPARQL update commands are in:
[../../sparql/](../../sparql/)


```
fix-bad-uris.upd.ql
move-triples-to-new-graph.rq
rename-uri.rq
rename-graph.rq
```

They can be run by `scripts/rupdate.sh` .


### Misc.
In the case when one wants to override some triples that are already loaded in graph G, and taking in account the language of the literals:

    java -cp $JARS deductions.runtime.jena.DataSourceManagerApp \
		file:///home/user/newTriples.tll 'http://jmvanel.free.fr/jmv.rdf#me'
    java -cp $JARS deductions.runtime.jena.DataSourceManagerApp \
		"https://raw.githubusercontent.com/assemblee-virtuelle/pair/master/form_labels.ttl" "rdf-i18n"
    # or:
    sbt "runMain deductions.runtime.jena.DataSourceManagerApp \
		https://raw.githubusercontent.com/assemblee-virtuelle/pair/master/form_labels.ttl rdf-i18n"

For each triple and each language ?lang, `?S ?P "val"@lang.` in newTriples.tll , this will remove the existing triple:  `?S ?P "old val"@lang.` , and add the new triple: `?S ?P "val"@lang.` 

## Dump and reload

For safety of data, it is good to make global database dumps sometimes. However, in 3 years of working with Jena TDB, we never lost data.
In case of pirate attacks, the dump can a way to recover correct data.

Dumping all database:

```
for f in lib/*.jar
do
  JARS=$JARS:$f
done
echo java -cp $JARS
java -cp $JARS tdb.tdbdump --loc=TDB | sed 's/^.\{,23\}// ; s/....$//' > dump.nq
```
Or with sbt:

    sbt "runMain tdb.tdbdump --loc=TDB" > dump.nq

To re-load the database from N-Quads format (possibly delete the TDB directory before) :

    sbt "runMain tdb.tdbloader --loc=TDB dump.nq"

See more details below.

### Example of a shell session to reload a dump

Here is a typical shell session to reload a Jena TDB quads dump (made with class `tdb.tdbdump` ).

See the doc for the TDB tools:
https://jena.apache.org/documentation/tdb/commands.html#tdbloader2

```
DUMP=/home/jmv/deploy/semantic_forms_play-2.1-SNAPSHOT/dump.nq
rapper -i nquads $DUMP
rapper: Parsing returned 35920 triples

source scripts/setClasspath.sh 
echo $JARS
:lib/aopalliance.aopalliance-1.0.jar:lib/com.fasterxml.aalto-xml-1.0.0.jar:lib/com.fasterxml.jackson.core.jackson-annotations-2.7.8.jar:lib/com.fasterxml.jackson.core.jackson-core-2.7.8.jar:lib/com.fasterxml.jackson.core.jackson-databind-2.7.8.jar:lib/com.fasterxml.jackson.datatype.jackson-datatype-jdk8-2.7.8.jar:lib/com.fasterxml.jackson.datatype.jackson-datatype-jsr310-2.7.8.jar:lib/com.github.andrewoma.dexx.collection-0.6.jar:lib/com.github.jsonld-java.jsonld-java-0.9.0.jar:lib/com.google.guava.guava-16.0.1.jar:lib/com.google.inject.extensions.guice-assistedinject-4.0.jar:lib/com.google.inject.guice-4.0.jar:lib/commons-cli.commons-cli-1.3.jar:lib/commons-codec.commons-codec-1.10.jar:lib/commons-io.commons-io-2.5.jar:lib/commons-logging.commons-logging-1.2.jar:lib/com.typesafe.akka.akka-actor_2.11-2.4.14.jar:lib/com.typesafe.akka.akka-slf4j_2.11-2.4.14.jar:lib/com.typesafe.akka.akka-stream_2.11-2.4.14.jar:lib/com.typesafe.config-1.3.0.jar:lib/com.typesafe.netty.netty-reactive-streams-1.0.8.jar:lib/com.typesafe.netty.netty-reactive-streams-http-1.0.8.jar:lib/com.typesafe.play.build-link-2.5.12.jar:lib/com.typesafe.play.play_2.11-2.5.12.jar:lib/com.typesafe.play.play-datacommons_2.11-2.5.12.jar:lib/com.typesafe.play.play-exceptions-2.5.12.jar:lib/com.typesafe.play.play-functional_2.11-2.5.12.jar:lib/com.typesafe.play.play-iteratees_2.11-2.5.12.jar:lib/com.typesafe.play.play-json_2.11-2.5.12.jar:lib/com.typesafe.play.play-netty-server_2.11-2.5.12.jar:lib/com.typesafe.play.play-netty-utils-2.5.12.jar:lib/com.typesafe.play.play-server_2.11-2.5.12.jar:lib/com.typesafe.play.play-streams_2.11-2.5.12.jar:lib/com.typesafe.play.twirl-api_2.11-1.1.1.jar:lib/com.typesafe.ssl-config-core_2.11-0.2.1.jar:lib/deductions.semantic_forms-1.0-SNAPSHOT.jar:lib/deductions.semantic_forms_play-1.0-SNAPSHOT-assets.jar:lib/deductions.semantic_forms_play-1.0-SNAPSHOT-sans-externalized.jar:lib/io.netty.netty-buffer-4.0.41.Final.jar:lib/io.netty.netty-codec-4.0.41.Final.jar:lib/io.netty.netty-codec-http-4.0.41.Final.jar:lib/io.netty.netty-common-4.0.41.Final.jar:lib/io.netty.netty-handler-4.0.41.Final.jar:lib/io.netty.netty-transport-4.0.41.Final.jar:lib/io.netty.netty-transport-native-epoll-4.0.41.Final-linux-x86_64.jar:lib/javax.inject.javax.inject-1.jar:lib/javax.servlet.servlet-api-2.4.jar:lib/javax.transaction.jta-1.1.jar:lib/joda-time.joda-time-2.9.6.jar:lib/log4j.log4j-1.2.17.jar:lib/net.rootdev.java-rdfa-0.4.2.jar:lib/org.antlr.antlr-runtime-3.5.jar:lib/org.apache.any23.apache-any23-api-1.1.jar:lib/org.apache.any23.apache-any23-csvutils-1.1.jar:lib/org.apache.commons.commons-collections4-4.1.jar:lib/org.apache.commons.commons-csv-1.4.jar:lib/org.apache.commons.commons-lang3-3.4.jar:lib/org.apache.httpcomponents.httpclient-4.5.2.jar:lib/org.apache.httpcomponents.httpclient-cache-4.5.2.jar:lib/org.apache.httpcomponents.httpcore-4.4.4.jar:lib/org.apache.httpcomponents.httpmime-4.3.1.jar:lib/org.apache.jena.jena-arq-3.2.0.jar:lib/org.apache.jena.jena-base-3.2.0.jar:lib/org.apache.jena.jena-cmds-3.2.0.jar:lib/org.apache.jena.jena-core-3.2.0.jar:lib/org.apache.jena.jena-iri-3.2.0.jar:lib/org.apache.jena.jena-permissions-3.2.0.jar:lib/org.apache.jena.jena-rdfconnection-3.2.0.jar:lib/org.apache.jena.jena-shaded-guava-3.2.0.jar:lib/org.apache.jena.jena-tdb-3.2.0.jar:lib/org.apache.jena.jena-text-3.2.0.jar:lib/org.apache.logging.log4j.log4j-api-2.8.jar:lib/org.apache.logging.log4j.log4j-core-2.8.jar:lib/org.apache.logging.log4j.log4j-slf4j-impl-2.8.jar:lib/org.apache.lucene.lucene-analyzers-common-4.9.1.jar:lib/org.apache.lucene.lucene-core-4.9.1.jar:lib/org.apache.lucene.lucene-demo-4.9.1.jar:lib/org.apache.lucene.lucene-expressions-4.9.1.jar:lib/org.apache.lucene.lucene-facet-4.9.1.jar:lib/org.apache.lucene.lucene-misc-4.9.1.jar:lib/org.apache.lucene.lucene-queries-4.9.1.jar:lib/org.apache.lucene.lucene-queryparser-4.9.1.jar:lib/org.apache.lucene.lucene-sandbox-4.9.1.jar:lib/org.apache.lucene.lucene-suggest-4.9.1.jar:lib/org.apache.solr.solr-solrj-4.9.1.jar:lib/org.apache.thrift.libthrift-0.9.3.jar:lib/org.apache.zookeeper.zookeeper-3.4.6.jar:lib/org.codehaus.woodstox.stax2-api-4.0.0.jar:lib/org.codehaus.woodstox.wstx-asl-3.2.7.jar:lib/org.joda.joda-convert-1.8.1.jar:lib/org.noggit.noggit-0.5.jar:lib/org.openrdf.sesame.sesame-model-2.7.10.jar:lib/org.openrdf.sesame.sesame-rio-api-2.7.10.jar:lib/org.openrdf.sesame.sesame-util-2.7.10.jar:lib/org.ow2.asm.asm-4.1.jar:lib/org.ow2.asm.asm-commons-4.1.jar:lib/org.reactivestreams.reactive-streams-1.0.0.jar:lib/org.scala-lang.modules.scala-async_2.11-0.9.6.jar:lib/org.scala-lang.modules.scala-java8-compat_2.11-0.7.0.jar:lib/org.scala-lang.modules.scala-parser-combinators_2.11-1.0.4.jar:lib/org.scala-lang.modules.scala-xml_2.11-1.0.6.jar:lib/org.scala-lang.scala-library-2.11.8.jar:lib/org.scala-lang.scala-reflect-2.11.8.jar:lib/org.scala-stm.scala-stm_2.11-0.7.jar:lib/org.scalaz.scalaz-core_2.11-7.2.8.jar:lib/org.slf4j.jcl-over-slf4j-1.7.21.jar:lib/org.slf4j.jul-to-slf4j-1.7.21.jar:lib/org.slf4j.slf4j-api-1.7.21.jar:lib/org.slf4j.slf4j-log4j12-1.7.21.jar:lib/org.w3.banana-jena_2.11-0.8.4-SNAPSHOT.jar:lib/org.w3.banana-rdf_2.11-0.8.4-SNAPSHOT.jar:lib/org.w3.ntriples_2.11-0.8.4-SNAPSHOT.jar:lib/xerces.xercesImpl-2.11.0.jar:lib/xml-apis.xml-apis-1.4.01.jar

java -cp $JARS -Xmx8G tdb.tdbloader --help

java -cp $JARS -Xmx8G tdb.tdbloader --loc=TDB --verbose $DUMP
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/home/jmv/apps/eulergui-2.1-SNAPSHOT-jar-with-dependencies.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/home/jmv/deploy/semantic_forms_play-1.0-SNAPSHOT/lib/org.apache.logging.log4j.log4j-slf4j-impl-2.8.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/home/jmv/deploy/semantic_forms_play-1.0-SNAPSHOT/lib/org.slf4j.slf4j-log4j12-1.7.21.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.Log4jLoggerFactory]
Java maximum memory: 7635730432
symbol:http://jena.hpl.hp.com/ARQ#constantBNodeLabels = true
symbol:http://jena.hpl.hp.com/ARQ#regexImpl = symbol:http://jena.hpl.hp.com/ARQ#javaRegex
symbol:http://jena.hpl.hp.com/ARQ#stageGenerator = com.hp.hpl.jena.tdb.solver.StageGeneratorDirectTDB@40005471
symbol:http://jena.hpl.hp.com/ARQ#strictSPARQL = false
symbol:http://jena.hpl.hp.com/ARQ#enablePropertyFunctions = true
INFO  -- Start triples data phase
INFO  ** Load empty triples table
INFO  -- Start quads data phase
INFO  ** Load into quads table with existing data
INFO  Load: /home/jmv/deploy/semantic_forms_play-1.0-SNAPSHOT_OLD2/dump.nq -- 2017/11/24 12:08:01 CET
ERROR [line: 6471, col: 1 ] Broken IRI (newline): http://www.w3.org/1999/02/22-rdf-syntax-ns15:59:16.986 [main] DEBUG c.h.h.j.t.b.file.BlockAccessMapped - Segment: 2
org.apache.jena.riot.RiotException: [line: 6471, col: 1 ] Broken IRI (newline): http://www.w3.org/1999/02/22-rdf-syntax-ns15:59:16.986 [main] DEBUG c.h.h.j.t.b.file.BlockAccessMapped - Segment: 2
```

After editing the dump to fix the syntax error:

```
java -cp $JARS -Xmx8G tdb.tdbloader --loc=TDB --verbose /home/jmv/deploy/semantic_forms_play-1.0-SNAPSHOT_OLD2/dump.nq
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/home/jmv/apps/eulergui-2.1-SNAPSHOT-jar-with-dependencies.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/home/jmv/deploy/semantic_forms_play-1.0-SNAPSHOT/lib/org.apache.logging.log4j.log4j-slf4j-impl-2.8.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/home/jmv/deploy/semantic_forms_play-1.0-SNAPSHOT/lib/org.slf4j.slf4j-log4j12-1.7.21.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.Log4jLoggerFactory]
Java maximum memory: 7635730432
symbol:http://jena.hpl.hp.com/ARQ#constantBNodeLabels = true
symbol:http://jena.hpl.hp.com/ARQ#regexImpl = symbol:http://jena.hpl.hp.com/ARQ#javaRegex
symbol:http://jena.hpl.hp.com/ARQ#stageGenerator = com.hp.hpl.jena.tdb.solver.StageGeneratorDirectTDB@40005471
symbol:http://jena.hpl.hp.com/ARQ#strictSPARQL = false
symbol:http://jena.hpl.hp.com/ARQ#enablePropertyFunctions = true
INFO  -- Start triples data phase
INFO  ** Load empty triples table
INFO  -- Start quads data phase
INFO  ** Load into quads table with existing data
INFO  Load: /home/jmv/deploy/semantic_forms_play-1.0-SNAPSHOT_OLD2/dump.nq -- 2017/11/24 12:11:09 CET
INFO  -- Finish triples data phase
INFO  -- Finish quads data phase
INFO  ** Data: 35,922 quads loaded in 1.80 seconds [Rate: 19,934.52 per second]
INFO  -- Start quads index phase
INFO  -- Finish quads index phase
INFO  -- Finish triples load
INFO  -- Finish quads load
INFO  ** Completed: 35,922 quads loaded in 1.82 seconds [Rate: 19,759.08 per second]
```

## Mirror SPARQL site into SF semantic cache

The input is a SPARQL query to another endpoint. This client program loads URI's returned by the query into SF.
In the end it is like if a user had pasted all those URI's and clicked on "Display".
It amounts to make linked copies of all these semantic URI's into SF's semantic cache.
It leverages in the /load-uri service in SF.

**Examples of running**

__Copy all recursive subclasses of given class__

The RDF dataset taxref.mnhn.fr of the french Museum National d'Histoire Naturelle (MNHN) models taxa (=taxon, species, genus, families, etc) as RDF classes.

The URI to be imported **must** be under a variable `?sub` .

the arguments are :

- SPARQL query 
- SPARQL endpoint
- ULR prefix of semantic_forms target instance

```shell
   java -cp $JARS deductions.runtime.clients.SPARQLquery2SFcacheApp \
    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> 
    SELECT * 
    WHERE {
     ?sub rdfs:subClassOf* <http://taxref.mnhn.fr/lod/taxon/185292/10.0> .
      ?sub rdfs:label ?LAB .  ?sub <http://taxref.mnhn.fr/lod/property/hasRank> ?RANK .
    } LIMIT 5" \
       http://taxref.mnhn.fr/sparql \
       http://localhost:9000
```

Of course, remove the limit for a real run.

__Copy all resources of a given rdf:type class__

The Lotico.com organization is dedicated to Semantic Web community since many years, and its Jena backed dataset holds almost 60_000 `foaf:Person`'s.

```shell
   java -cp $JARS deductions.runtime.clients.SPARQLquery2SFcacheApp \
    "PREFIX foaf: <http://xmlns.com/foaf/0.1/> 
    SELECT * 
    WHERE {
     ?sub a foaf:Person .
    } LIMIT 5" \
       http://www.lotico.com:3030/lotico/query \
       http://localhost:9000
```

Or, from the SBT console:
```scala
   val sparql = """
    PREFIX foaf: <http://xmlns.com/foaf/0.1/> 
    SELECT * 
    WHERE {
     ?sub a foaf:Person .
    } """
   deductions.runtime.clients.SPARQLquery2SFcacheApp . main(
     Array(
       sparql,
       "http://www.lotico.com:3030/lotico/query",
       "http://localhost:9000" ))
```

__Copy all dbo:Garden from dbPedia__

The query on dbPedia is:
```sparql
PREFIX dbo: <http://dbpedia.org/ontology/>
select ?G where {
  { ?G a dbo:Garden . }
  UNION
  { ?G a <http://dbpedia.org/class/yago/Garden103417345> . }
}
```
The endpoint is http://dbpedia.org/sparql .

# Semantize raw stuff

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
  *  		`<any:ROW> a foaf:Person .`
  *    which will add this triple to every row.
 
The features are like Any23, plus:
 * abbreviated Turtle terms with well-known prefixes (eg foaf:name) are understood as columns names 
 * abbreviated Turtle terms with well-known prefixes (eg dbpedia:Paris) are understood in cells 

For example, adding `rdf:type` in first row and `foaf:Person` in another row will assign the class  `foaf:Person` to (the URI of) this row.

See more detail here in the SF wiki:
https://github.com/jmvanel/semantic_forms/wiki/Semantization
 
Link on [other tools for semantization](http://svn.code.sf.net/p/eulergui/code/trunk/eulergui/html/documentation.html#L3234).

# SPARQL queries
There is a web page for SPARQL queries, and also a real (compliant) SPARQL endpoint at URL `/sparql` for CONSTRUCT and SELECT queries (see in README in parag. Test about how to query with authentication).

There is an HTML page for entering queries, in the /tools page.
There are some example showing some queries that you can paste into your browser:

The service
<code>/sparql-ui?query=</code>
is for CONSTRUCT queries.

and the service
<code>/select-ui?query=</code>
is for SELECT queries.

In the /tools page, there is also a link to the YasGUI client tool, a famous SPARQL requester.

In addition to the SPARQL endpoint at URL `/sparql`, therer is another endpoint at URL `/sparql2`, where all named graphs content is put in the unnamed graph. This is convenient:

- many tools do not consider named graphs (or just one), like Sparklis,
- one does not have to wrap all triple patterns in `GRAPH ?GRnnn { }` blocks

# Populating with dbPedia (mirroring dbPedia)

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

# Text indexing with Lucene or SOLR

For details please look the Jena TDB documentation about text search with Lucene or SOLR in TDB :
https://jena.apache.org/documentation/query/text-query.html

If you run semantic\_forms from scratch, text search with Lucene is activated by default.
You have nothing to do, the necessary Lucene libraries are present, and the RDF content added is indexed in Lucene as soon as it is added.

The settings for text search with Lucene or SOLR in TDB here:
https://github.com/jmvanel/semantic\_forms/blob/master/scala/forms/src/main/scala/deductions/runtime/jena/LuceneIndex.scala
https://github.com/jmvanel/semantic\_forms/blob/master/scala/forms/src/main/scala/deductions/runtime/services/DefaultConfiguration.scala

If you deactivate `useTextQuery` in DefaultConfiguration.scala, the text search is done by a plain SPARQL search, that considers input as a regular expression.

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

