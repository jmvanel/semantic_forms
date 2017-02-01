# Introduction
̀Semantic_forms` est un projet Open Source d'application de gestion de données et navigation du LOD.
https://github.com/jmvanel/semantic_forms/releases
https://github.com/jmvanel/semantic_forms/wiki

Autour de Semantic_forms on fera :
- initiation au Linked Open Data (alias Web Sémantique)
- un atelier de déploiement d'un projet perso. avec
  - choix d'un modèle de données
  - création de formulaires
  - affichage de rendu via requête SPARQL , avec ou sans templating

# Originalités de  ̀Semantic_forms`
Souvent, le logiciel libre est à la traîne du commercial. Pour SF, ce n'est pas le cas!
- navigation sur le LOD sans quitter l'application
- modèles de données (OWL) totalement flexibles y compris pour l'utilisateur final
- capacités d'annotation illimitées sans détruire les données LOD importées in les donnes saisies
- totalement dans l'esprit et les recommandations du W3C: cardinalité libre, I18N

# l'application générique
- navigation du LOD (Linked Open Data)
- édition, création à partir d’une classe OWL
- capacités collaboratives
- gestion des graphes nommés et cache sémantique
- la recherche étendue
- la recherche avec Lucene

Principes généraux de l'IHM
- utilisation généralisée du glisser-déposer
- utilisation généralisée des infobulles (actions possibles et données)

## Navigation et edition du LOD (Linked Open Data)
Sur ces sujets :
- gestion des graphes nommés et cache sémantique
- la recherche étendue
voir:
https://github.com/jmvanel/semantic_forms/wiki/Manuel-utilisateur

## Capacités collaboratives
- l'historique des modifications
- la recherche dépend de ce qui a été consulté

# Les capacités de framework
- le service de formulaires /form
- le service de données enrichies /form-data
- création de formulaires de spécification
- gestion des données utilisateur: saisie, compte
Sur ces sujets, voir:
https://github.com/jmvanel/semantic_forms/wiki/Application-development-manual

# Spécifications du W3C
- La création, la recherche et la réutilisation d'ontologies OWL
- initiation à SPARQL
- RDFa
- LDP
