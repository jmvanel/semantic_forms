<!-- pandoc --standalone install_party_semantic_forms.md > install_party_semantic_forms.html -->

# Install party `semantic_forms`

`semantic_forms` est une application générique de navigation du [LOD](https://fr.wikipedia.org/wiki/Linked_open_data) (**Linked Open Data**).
Plus encore, c'est une application d'annotation, d'édition de données structurées, et un cadriciel (framework) pour construire des **applications d'entreprise** centrées sur les formulaires (entrée ou lecture seule). C'est un pas vers le Système d'Information Sémantique, qui est destiné à remplacer les PGI ([Progiciel de Gestion Intégré](https://fr.wikipedia.org/wiki/Progiciel_de_gestion_int%C3%A9gr%C3%A9), ERP). Toutes les données et tous les modèles de données (ontologies) s'appuient sur les recommandations du **Web Sémantique du W3C**: 
[`wikipedia / Semantic_Web`](https://en.wikipedia.org/wiki/Semantic_Web).

Le Web Sémantique, et `semantic_forms`, s'inscrivent dans un vaste mouvement visant à se réapproprier ses **données personnelles**, qui sont actuellement épar-pillées ;) dans des silos relativement étanches: Facebook, LinkedIn, Google, etc. 
Cf le projet [Solid](https://blog.p2pfoundation.net/solid-can-web-re-decentralised/2016/04/07), “social linked data”.

La flexibilité des bases de données [SPARQL](https://fr.wikipedia.org/wiki/SPARQL) du web Sémantique, leur excellente standardisation par le W3C, la présence de modèles de données réutilisables (FOAF, schema.org, dublin Core, SIOC ...), offrent aux entreprises et associations un excellent support pour leurs applications, et pour mutualiser les données quand le besoin se fait sentir. Le standard JSON-LD ([JSON pour le Linked Data](https://en.wikipedia.org/wiki/JSON-LD)) permet d'échanger des données en favorisant les liens entre données et la gouvernance décentralisée plutôt que les recopies.

`semantic_forms` peut aussi être vu comme un outil d'administration de bases SPARQL, même s'il n'y a pas encore toutes les fonctionnalités. Le cache sémantique, le support (partiel) du protocole LDP, permettent d'agréger de manière collaborative des documents provenant de tout le LOD.

De plus, la qualité des modèles de données sémantiques (appelés ontologies ou vocabulaires), ainsi que la scalabilité de certaines bases SPARQL, permettent de supporter facilement les études **Big Data et Intelligence Economique** (BI). C'est à dire que l'étape de préparation d'un modèle de type OLAP, ou Hadoop, ou Spark Apache, très consommatrice en resources humaines, n'a plus lieu d'être. On travaille directment sur la base SPARQL

## Public visé
On veut intéresser plusieurs publics:

- les curieux du Web Sémantique,
- ceux qui veulent créer une application rapidement, façon RAD (Rapid Application Development),
- les amateurs du langage Scala

## Pré-requis
- pour utiliser le bac à sable, juste un ordinateur,
- pour installer en local, juste Java 8 JRE, voir [installation](install.md)
- pour installer à partir des sources, voir [README](../../scala/forms_play/README.md)

## Activités
`semantic_forms` est facilement installable sur un ordinateur (installer Java JRE 8, dézipper la distribution, lancer l'application). Un "bac à sable" est accessible en permanence. En plus une instance dédiée à l'évènement sera créée, qui permettra d'interagir comme un réseau social.

Les activités possibles sont:

- initiez vous au LOD (Linked Open Data) et au Web Sémantique à travers `semantic_forms` ; voir le <a href="https://github.com/jmvanel/semantic_forms/wiki/Manuel-utilisateur">Manuel utilisateur</a>
    * Naviguer
    * charger des données
    * créer et modifier des objets (personnes, projets, etc)
    * créer votre profil FOAF (carte de visite sémantique)
    * afficher des graphes (noeuds et liens)
    * administrer la base SPARQL


- venez avec vos données (TTL, CSV, XML, JSON ),
    1. sémantisez les (voir [Sémantisation de CSV, etc](../../scala/forms_play/README.md#Semantize raw stuff)),
    2. via `semantic_forms` vous avez une application de consultation + édition;
    3. éventuellement créez des spécifications de formulaires ; ; voir [manuel du dévelopment d'application](https://github.com/jmvanel/semantic_forms/wiki/Application-development-manual)
    4. éventuellement personnalisez les pages de l'application: composer des pages Web statiques qui appellent via JavaScript un ou plusieurs services Web de formulaires
- venez avec vos modèles de données ( OWL, SQL, UML, XML Schéma, Markdown, CSV ) et installez une application consultation + édition;
- venez avec un projet d'application, et créez un modèle de données en s'appuyant sur les vocabulaires existant du Web Sémantique, et des spéc. de formulaires
- initiez vous à Scala, Play!, à travers le cadriciel `semantic_forms`; voir [construire à partir des sources](../../scala/forms_play/README.md#Run locally from sources)

En tous cas, [installer la distribution `semantic_forms`](install.md), localement ou sur un serveur avec connection SSH, est très simple: déziper, lancer le script (il faut juste avoir Java 8).


