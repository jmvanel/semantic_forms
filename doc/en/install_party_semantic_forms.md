<!-- pandoc --standalone install_party_semantic_forms.md> install_party_semantic_forms.html -->

# Install party `semantic_forms`

`Semantic_forms` is a generic application navigation of [LOD](https://fr.wikipedia.org/wiki/Linked_open_data) (** Linked Open Data **).
Moreover, it is an application for annotation, editing of structured data, and a software framework to build **business applications**, forms centric (single entry or reading). This is the Semantic Information System, which is designed to replace PGI ([Enterprise Resource Planning] (https://fr.wikipedia.org/wiki/Progiciel_de_gestion_int%C3%A9gr%C3%A9), ERP) . All data and all data models (ontologies) are based on the recommendations of the W3C Semantic Web ** **:
[ `Wikipedia / Semantic_Web`] (https://en.wikipedia.org/wiki/Semantic_Web).

The Semantic Web, and `semantic_forms`, are part of a broad movement to reclaim his personal data ** **, currently epar-looted;) in relatively tight silos: Facebook, LinkedIn, Google, etc.
Cf the project [Solid] (https://blog.p2pfoundation.net/solid-can-web-re-decentralised/2016/04/07), "Social linked data".

The flexibility of [SPARQL] databases (https://fr.wikipedia.org/wiki/SPARQL) Semantic web, their excellent standardized by the W3C, the presence of reusable data models (FOAF, schema.org, dublin Core ... SIOC), offer companies and associations excellent support for their applications, and to pool data when the need arises. The JSON-LD standard ([JSON for Linked Data] (https://en.wikipedia.org/wiki/JSON-LD)) enables the exchange of data by encouraging links between data and decentralized governance rather than recopies.

`Semantic_forms` can also be seen as an administration tool bases SPARQL, even if there is not yet full functionality. The semantic cache, support (partial) of the LDP, allow to aggregate collaboratively documents from all the LOD.

Moreover, the quality of semantic data models (called ontologies and vocabularies), and the scalability of some bases SPARQL, make it easy to support Big Data and studies ** ** Intelligence (BI). Ie that the step of preparing an OLAP model type, or Hadoop, Spark or Apache, very consuming human resources, no longer relevant. directment working based SPARQL

## Targeted audience
We want more public interest:

- The curious Semantic Web,
- Those who want to build an application quickly, so RAD (Rapid Application Development)
- Lovers of language Scala

## Requirements
- To use the sandbox, just a computer,
- To install locally, just 8 Java JRE, see [Setup] (install.md)
- To install from source, see [README] (../../ scala / forms_play / README.md)

## Activities
`Semantic_forms` is easily installed on a computer (install Java JRE 8, unzip the distribution, launch the application). A "sandbox" is permanently accessible. In addition a dedicated instance for the event will be created, which will interact as a social network.

Possible activities are:

- Try your hand at LOD (Linked Open Data) and the Semantic Web through `semantic_forms`; see <a href="https://github.com/jmvanel/semantic_forms/wiki/Manuel-utilisateur"> </a> User Manual
    * Navigate
    * Load data
    * Create and modify objects (people, projects, etc.)
    * Create your profile FOAF (semantic business card)
    * Display graphs (nodes and links)
    * Administer the database SPARQL


- Come with your data (TTL, CSV, XML, JSON)
    1. sémantisez them (see [CSV semanticization, etc.] (../../ scala / forms_play / README.md # Semantize raw stuff))
    2. via `semantic_forms` you have a consultation + application publishing;
    3. possibly create specification forms; ; See [of Application Development Manual] (https://github.com/jmvanel/semantic_forms/wiki/Application-development-manual)
    4. possibly customize pages in the application: call static Web pages that require JavaScript via one or more Web services forms
- Come with your data model (OWL, SQL, UML, XML Schema, Markdown, CSV) and install a consultation + application publishing;
- Come with an application project, and create a data model based on the existing Semantic Web vocabularies, and spec. forms
- Try your hand at Scala, Play !, through cadriciel `semantic_forms`; see [build from source] (../../ scala / forms_play / README.md # Run locally from sources)

In any case, [install the `semantic_forms` distribution] (install.md), locally or on a server with SSH connection is very simple: unzip, run the script (just have Java 8).
Google Traduction pour les entreprises :Google Kit du traducteurGadget TraductionOutil d'aide à l'export
À propos de Google TraductionCommunautéMobile
À propos de GoogleConfidentialité et conditions d'utilisationAideEnvoyer des commentaires

