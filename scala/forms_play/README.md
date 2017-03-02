Play! framework implementations
---

# Introduction
Here is a web application with Play! framework around the [form generator](../forms/README.md) that does:
- navigation on the LOD (Linked Open Data) cloud,
- CRUD (CReate, Update, Delete) editing,
- search
- semantic cache

For now, the display looks like this, 
plus a textbox to enter a URL semantics, eg a FOAF profile or DBpedia URI : 
[example.form.foaf.html](http://htmlpreview.github.io/?https://github.com/jmvanel/semantic_forms/blob/master/scala/forms/example.form.foaf.html)

This README is like an administrator Manual.
For other documentations, see:

- the [wiki](https://github.com/jmvanel/semantic_forms/wiki) for User manual and Developer manual.
- [Installation of the `semantic_forms` generic application](../../doc/en/install.md) from zip distribution (easy and no compilation)


## Terminology

Some people speak of "triple store", or "graph database", or "triple database", or "SPARQL database". We will write here just "database".

# How to run
## Run locally from sources

- install dependencies:
  - Java 8 ,
  - [SBT](http://www.scala-sbt.org/) or [Typesafe Activator](http://typesafe.com/platform/getstarted) .
```shell
wget https://dl.bintray.com/sbt/native-packages/sbt/0.13.13/sbt-0.13.13.tgz
tar xvzf sbt-0.13.13.tgz
# or use Linux package manager if you prefer:
sudo apt-get install sbt
```

Then SBT will download the rest.
- download the source from [Banana-RDf fork on github](https://github.com/deductions/banana-rdf) (temporary, until my Pull Request in Banana-RDF is accepted)
  - build this project with SBT : change directory to `banana-rdf` ; type in the SBT console :
```
       ++2.11.8
       publishLocal
```
- download the source from [semantic\_forms on github](https://github.com/jmvanel/semantic_forms/)
- build and run the `semantic_forms` project itself with SBT: change directory to `scala/forms_play` within the source downloaded from github;
- type in the SBT console : `~ run`

The default port is 9000, so you can direct your browser to [http://localhost:9000](http://localhost:9000) .
To run on another port than 9000 :

    run 9053

To use HTTPS, also just change the port to 9443, see:
https://www.playframework.com/documentation/2.4.x/ConfiguringHttps


To raise the memory, set this environment variable:

    export SBT_OPTS="-Xmx2G"

To load some common vocabularies (FOAF, ...) and form specifications, see below "Preloading RDF content".
To understand usage of the user interface, see the [User manual wiki](https://github.com/jmvanel/semantic_forms/wiki).

To build the ScalaDoc, run `sbt doc` , and the ScalaDoc will be in

	target/scala-2.11/api/

A convenience link to [local semantic\_forms ScalaDoc](../forms/target/scala-2.11/api/index.html)


Some references on ScalaDoc: http://docs.scala-lang.org/style/scaladoc.html , http://docs.scala-lang.org/overviews/scaladoc/for-library-authors.html , http://stackoverflow.com/questions/15394322/how-to-disambiguate-links-to-methods-in-scaladoc

## Run without development environment
There are several use cases:

- server (internet or intranet)
- personal usage
- running some memory intensive scripts (avoids the big overhead of SBT)

For both usages, the install is the same (and very easy).
See the [installation doc.](https://github.com/jmvanel/semantic_forms/blob/master/doc/en/install.md#installation-of-the-semantic_forms-generic-application)

The personal usage can be a contact manager, project manager, blog, or notes manager, ... It allows to create one's FOAF profile, navigate on the Web of Data and keeping track, or any structured data management. See details on 
 [User manual / possible usages](https://github.com/jmvanel/semantic_forms/wiki/User_manual#possible-usages)

### Obtaining the zipped application
The zipped application is available as a [github release](https://github.com/jmvanel/semantic_forms/releases).

Otherwise, to obtain the zipped application when starting from the sources (see above), type in SBT : `dist`

Then the archive is found here :
`target/universal/semantic_forms_play-1.0-SNAPSHOT.zip`

### Runnning the zipped application
For more details, , see: [installation doc.](../../doc/en/install.md)

Download this zip on the server, unzip and type:
```shell
cd semantic_forms_play-1.0-SNAPSHOT
nohup bin/semantic_forms_play -J-Xmx50M &
```

By doing this you start a web serveur on your machine. The default port is 9000, so you can direct your browser to [http://localhost:9000](http://localhost:9000) .

The generic application is perfectly usable out of the box for navigating, see [User manual](https://github.com/jmvanel/semantic_forms/wiki/User_manual). However, for editing data, it is better to preload common RDF vocabularies and related form specifications and I18N translations, see:
[installation doc.](../../doc/en/install.md)
and [preloading-rdf-content](#preloading-rdf-content) .

To check that the external service dbpedia-lookup is working
http://rubenverborgh.github.io/dbpedia-lookup-page/

#### Settings when runnning the zipped distribution
For more details, , see: [installation documentation](../../doc/en/install.md)
and [administration documentation](../../doc/en/administration.md).
You can change the default port (9000) to e.g. 9999 like this:

	nohup bin/semantic_forms_play -J-Xmx50M -Dhttp.port=9999 &

There is no need to be administrator.


# Setting a IDE project ( eclipse ...)

Please read explanations on the Banana-RDF project:
[ide-setup](https://github.com/w3c/banana-rdf/#ide-setup)

Note that you run
```
sbt eclipse
```
just once in the directory scala/forms\_play/ ; this creates configuration for 2 eclipse projects: one in directory forms\_play/, one in directory forms.

You may have to set by hand the dependency in project forms\_play/ towards the other. For this, go in Properties of project, Java Build Path, Projects tab, and Add...

You may have to set by hand the dependency towards the Scala and Java code generated by Play: set this as source folder:

    target/scala-2.11/routes/main/
    target/scala-2.11/twirl/main/

## Ensime

Here are commands I used for installing Ensime for Vim
https://github.com/ensime/ensime-vim

```shell
sudo apt-get install python-pip
sudo pip install websocket-client
export BROWSER=firefox
echo 'export BROWSER=firefox' >> ~/.bashrc 
mkdir -p ~/.sbt/0.13/plugins/
echo 'addSbtPlugin("org.ensime" % "ensime-sbt" % "0.3.3")'     >> ~/.sbt/0.13/plugins/plugins.sbt
curl -fLo ~/.vim/autoload/plug.vim --create-dirs https://raw.githubusercontent.com/junegunn/vim-plug/master/plug.vim
sbt gen-ensime
```

## Troubleshooting & tips

- FAILED DOWNLOADS messages for the first SBT build: retry later. With SBT, as with all these dependency managers ( Maven, NMP), given the large amount of downloading from multiple sources, it is often the case that the first time, not everything is there.
- in case of troubles in build, delete `target/` directory
- create eclipse configurations with the "eclipse" command in sbt, in the project directory scala/forms\_play/ :

```
    eclipse with-source=true
```
- to remove the red errors in eclipse in Play! project, apply this workaround: http://stackoverflow.com/posts/28551583/revisions

Useful SBT plugins you may install for developppement purposes (add in ~/.sbt/0.13/plugins/plugins.sbt ):

	addSbtPlugin("org.ensime" % "ensime-sbt" % "1.9.0")
	addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")
	addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.0")

Commands to run with these useful plugins:

	dependencyUpdates
	dependencyTree
	test:dependencyGraph

See: https://github.com/jrudolph/sbt-dependency-graph , https://github.com/rtimush/sbt-updates

The jconsole command in the Java JDK is useful to monitor a running application like semantic\_forms, through SBT or standalone.
See https://docs.oracle.com/javase/8/docs/technotes/guides/management/jconsole.html

## Debug
See [playframework documentation/2.4.x/IDE](https://www.playframework.com/documentation/2.4.x/IDE)

Start SBT like below; then type run. Then start a remote debug in eclipse or another IDE with port 9999.
```shell
export SBT_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=9999" && sbt
```

## Test
```script
 get --method=OPTIONS --save-headers http://localhost:9000/bla

 # Do this (just once) to test URL that are protected by login:
 wget --keep-session-cookies --save-cookies cookies.txt \
    --post-data 'userid=foo&password=bar' \
    -p http://localhost:9000/authenticate

# If the user is not already created:
 wget --keep-session-cookies --save-cookies cookies.txt \
    --post-data 'userid=foo&password=bar&confirmPassword=bar' \
    -p http://localhost:9000/register

# test SPARQL CONSTRUCT service
wget --load-cookies cookies.txt \
    --header "Accept: application/ld+json" \
    'http://localhost:9000/sparql?query=CONSTRUCT{?S ?P ?O} WHERE{ GRAPH ?G {?S ?P ?O}} LIMIT 10'
```

The typical content of a cookie file is:
```
# HTTP cookie file.
# Generated by Wget on 2016-12-02 09:02:22.
# Edit at your own risk.

localhost:9000  FALSE   /       FALSE   0       PLAY_SESSION    f80b8a633c4debe1871ca7f23195b9766414fb79-username=foo
```


# Administration of a server instance from the sources
There is a script that updates the server from the sources, and more: it stops the server, updates the application from sources, and restarts the server :

    ./scripts/update_server.sh

It is advised to deactivate the automatic formatting on a server machine. Just comment out the line `scalariformSettings` in 
scala/forms/build.sbt . 

If you want to change the HTTP ports, etc, look in the Play! documentation:
https://www.playframework.com/documentation/2.4.x/ProductionConfiguration


## Change the log settings

`semantic_forms` uses Log4J 2 for logging. By default it prints very little.
If you want to apply your log settings, copy one the configuration files in conf/ (or use one as is):
```shell
cp conf/log4j2.debug.properties myconf.properties
vi myconf.properties
nohup bin/semantic_forms_play -Dlog4j.configurationFile=myconf.properties -mem 50 &
```

To download Java from the server with no browser (see http://stackoverflow.com/questions/10268583/downloading-java-jdk-on-linux-via-wget-is-shown-license-page-instead):

    VERSION=51
    wget --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" \
    http://download.oracle.com/otn-pub/java/jdk/8u$VERSION-b16/jdk-8u$VERSION-linux-arm-vfp-hflt.tar.gz

or for Linux x86:

    wget --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" \
    http://download.oracle.com/otn-pub/java/jdk/8u91-b14/jdk-8u91-linux-x64.tar.gz

    wget --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" \
    http://download.oracle.com/otn-pub/java/jdk/8u91-b14/jdk-8u91-linux-i586.tar.gz


#Database Administration

See [administration page](../../doc/en/administration.md)


# Vocabulary for forms


Here is the OWL vocabulary for ontolgy aware forms, currently used in this application:
[vocabulary/forms.owl.ttl](../../vocabulary/forms.owl.ttl)

It is useful to have, upstream of the FA (Abstract Form) in defined class FormModule.FormSyntax, an RDF vocabulary for forms.
It is not reusing Fresnel, but it could be aligned to Fresnel is necessary.

See also (in french) :
[doc/fr/formulaires.html](http://htmlpreview.github.io/?https://github.com/jmvanel/semantic_forms/blob/master/doc/fr/formulaires.html)

# Customize the plain vanilla application

semantic\_forms is really a framework, so it is made to override any feature.

The recommended way is to fork the project on github.
This fork that will never be merged (no Pull Request). It is a way to overcome the fact that there is no way (AFAIK) to redefine elements of a Play! project from an independent project.

To update with respect to semantic\_forms, see:

https://help.github.com/articles/syncing-a-fork/

And we must first do once and for all in every working directory from a git clone:
https://help.github.com/articles/configuring-a-remote-for-a-fork/

	git remote add upstream https://github.com/jmvanel/semantic_forms.git

An exemple of such a project is:
[https://github.com/deductions/semantic\_forms-sharecop](https://github.com/deductions/semantic_forms-sharecop)

See also:
[semantic\_forms wiki: Application-development-manual](https://github.com/jmvanel/semantic_forms/wiki/Application-development-manual)

# Community

For discussions that don't fit in the issues tracker, you may try 
the #eulergui IRC channel on freenode using a dedicated IRC client like XChat connecting to 
[irc://irc.freenode.net:6667/eulergui](irc://irc.freenode.net:6667/eulergui)
,
 or using the freenode HTML interface, for quick real time socializing.

Or [https://gitter.im/jmvanel/semantic\_forms](https://gitter.im/jmvanel/semantic_forms)

## Contributing

To ensure that all commits are formatted in a consistent way, it is strongly advised to use scalariform.

Please read how-to on the Banana-RDF project:
[ide-setup](https://github.com/w3c/banana-rdf/#contributions)


# The features 

The features are listed here for convenience, but from now on, we manage features on 
[Github issues](https://github.com/jmvanel/semantic_forms/issues).

## Features list 
- 1. a SPARQL 1.1 server available - DONE
- 2. user enters an URI and form view appears with the data from Internet - DONE
- 2.1 user enters an URI and form view appears with the data from the SPARQL server (RDF cache) - DONE
- 3. URIs in the form can be clicked to display another form with the data from Internet - DONE
- 4. entering new triples for existing properties, as in DataGUI or as in Ontowiki: http://aksw.org/source/edit ; by JavaScript - DONE
- 5. introduce the RDF cache, - implemented - DONE: unit test
- 6. creation of a new URI infering form from its class, as DomainApplication does - DONE
- 10 simple vocab' to specify properties by class in form - DONE
- 9. creating or editing URI's : propose URI's in relation with rdfs:domain class value; by JavaScript; could use the timestamps to order the URI's - DONE
- 13 Migration to Banana 0.7  - DONE ( 0.8.2-SNAPSHOT )
- 17 View SPARQL select & construct results : DONE
- 22 Add a button to edit the currently displayed URI : DONE
- 24   (HTML) : add CSS classes for labels and values; - DONE
- 24.1 (HTML) : new HTML output with CSS rendering instead of explicit HTML table formatting - DONE
- 25   (HTML) : add component to enter a dbpedia URI ( use dbpedia lookup API , by JavaScript ) - DONE
- 27   (HTML) : add component to enter a choice (single or multiple) for owl:OneOf classes ( by JavaScript or HTML 5 ) - DONE
- 28 for each URI, display a summary of the resource (rdfs:label, foaf:name, etc, depending on what is present in instance and of the class) instead of the URI
- 38 RDF Vocabulary for forms : DONE ; with details for each field ( see below ) : TODO
- 8. datatype validation : integer, date, telephone, .. ( by HTML5 CSS )
- 15 ScalaJS migration
- 20 Inferences for forms : implement owl union
- 21 write some JavaScript samples to call the different features
- 31 write a small help page explaining the role of local database in relation with the external data downloaded
- 32 TEST : Gatling stress scenario(s)
- 40 popup an editor for editing large texts
- 2.2 show statistics about the current document : # of triples, # of properties, # of URI's
- 19 Add simple login : WIP, also done in https://github.com/jmvanel/corporate\_risk/
- 19.1 record who did what : a solution is to use a named graph for each user  : done in https://github.com/jmvanel/corporate\_risk/
- 20 Inferences for forms : eliminate archaic properties
- 32 TEST : write Selenium scenario(s)

## Unimplemented features 
The features are listed here for convenience, but from now on, we manage features on 
[Github issues](https://github.com/jmvanel/semantic_forms/issues).

- 2.3 when document pointed by URI entered by user has no triple with that URI as subject, show a list of URI's in the document like the search results
- 7. use HTTP HEAD to distinguish RDF content types and others, and have different hyperlinks and styles for HTML, RDF, and image URL's - WIP
- 7.1 have icons to distinguish content types, to display near hyperlinks for HTML, RDF, image, etc URL's
- 11 button to remove a triple; by JavaScript - TODO
- 12 Integrate non-blocking: Future, Iteratee Enumerator -WIP
- 14 Migration to BigData(R) - TODO
- 16 Dashboard : # of triples, # of of resources; # of resources of each type - TODO
- 17.1 add FlintSPARQLEditor, but this implies to launch SPARQL HTTP server like Fuseki, or BigData, or to use the SPARQL protocol in the semantic\_forms server
- 18 Search : search also in URI strings - TODO
- 23 display the "reverse" triples ( called in-going links in BigData workbench ) : TODO , but there is a reverse links button on each triple
- 26 (HTML) : add component to enter an ordered RDF list : use same mechanism as multiple values, but send a Turtle list in parentheses; by JavaScript : TODO
- 28 for each URI, display a summary of the resource (rdfs:label, foaf:name, etc : this can use existing specifications of properties in form by class : [foaf.form.ttl](../forms/form_specs/foaf.form.ttl)
- 29 have a kind of merge in case of diverging modifications in the local endpoint and the original URI : TODO
- 30 (from Dario) : Separation of the attributes of a peer and the list of connected peers: on the left a list with the peer in question and all peers (connected) in its ecosystem and on the right a list of attributes the selected peer : TODO
	* in the left list one should be able to click on a peer so that it becomes the selected peer and its ecosystem appears (and updating on the right with its attributes)

- 33 framework to orchestrate a series of questions to user when the data is not present in database : in https://github.com/jmvanel/corporate\_risk/ there are form groups and automatic navigation to  next form by SAVE button - TODO componentize this
- 35 enforce mandatory properties ( by JavaScript )
- 36 display properties of blank nodes being objects of current form subject : TODO
- 37 datatype date : display a calendar ( by JavaScript ) : TODO
- 39 custom HTML : have an easy way to customize generated HTML forms and fields, by JavaScript or HTML : TODO

