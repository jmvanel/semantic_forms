<!-- pandoc --standalone install_party_semantic_forms.md > install_party_semantic_forms.html -->

# Install party `semantic_forms`

`semantic_forms` est une application générique de navigation du LOD.
Plus encore, c'est une application d'annotation, et un cadriciel (framework) pour construire des applications d'entreprise centrées sur les formulaires (entrée ou lecture seule). Toutes les données et tous les modèles de données (ontologies) s'appuient sur les recommandations du Web Sémantique du W3C: 
[`https://en.wikipedia.org/wiki/Semantic_Web`](https://en.wikipedia.org/wiki/Semantic_Web).

Le Web Sémantique, et `semantic_forms`, s'inscrivent dans un vaste mouvement visant à se réapproprier ses données personnelles, qui sont actuellement épar-pillées ;) dans des silos relativement étanches: Facebook, LinkedIn, Google, etc.

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
Donc les activités possibles sont:

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


