Développements IHM, CSS, et JS
==============================

Pour un hackaton!

# RDFViewer
**But**
Issues 
[Diagram (graph drawing) enhancements #91](https://github.com/jmvanel/semantic_forms/issues/91)

# IHM
## CSS formulaires

**But**
Pouvoir changer l'apparence des formulaires sans changer le code Scala 

**Explication**
On a le mécanisme suivant. Chaque élément du formulaire:

- libellé
- champ de saisie
- div pour une propriété (libellé + champ)
et chaque élément autour du formulaire:
- nom de l'objet
- classe pour création

vient avec une certaine classe.
Ces classes peuvent être changées dans la config. en Scala (bientôt config. en Turtle).

Ou alors, on change les CSS en vigueur (actuellement bootstrap).
Idéalement, on aimerait pouvoir, en CSS, dire "la classe c1 renvoie vers c2" .

## Vue tabulaire
Il y a aussi une partie serveur, qui renvoie la table à partir d'une classe, ou d'une requête SPARQL.
## Vue arborescente
Il y a aussi une partie serveur, qui renvoie l'arbre à partir d'un URI racine et éventuellement une liste de propriétés à utiliser (par exemple rdfs:subClassOf)

## Classes abtraites
Une classe abtraite est une classe qui a des sous-classes connues; une onstance concrète ne devriat pas être attachée uniquement à une classe abtraite.
Le moins que l'on puise faire, est de mettre an premier les sous-classes dans la liste pour la propriété rdf:type.
De cette façon, l'utilisateur pourra facilement affecter la bonne classe.

## Décorations diverses
Pour chaque fonctionnalité, ça peut être fait côté serveur ou IHM.
Je pencherai pour le faire en Scala, de telle façon que ça soit mettable dans l'IHM.
 
Par exemple:
- montrer un petit icône et lien Wikipedia pour les URI dbPedia, dans les réponses de la recherche /wordsearch
- montrer les images  comme des vignettes
- distinguer les URI "internes" LDP et les URI externes
## Placement en ligne des des URI dans la vue affichage
 
# Développements d'applications utilisant l'API Web /form

**But**
Voir comment utiliser l'API /form pour écrire une petite application simple.

## Exemple 1: suggestions de musiques et vote
- des danseurs se voient proposer des styles et/ou des morceaux; ils votent ( cochent ) ce qu 'ils veulent
- une autre page montre le résultat du vote

La page 2 va utiliser une requête SPARQL qui renvoie un résultat en JSON-LD.

## Exemple 2: suggestions d'objets proches
- une page renvoie les objets proches d'un objet donné; les résultats sont objets prochesclassés par types.

La page va utiliser une requête SPARQL qui renvoie un résultat en JSON-LD.

# Nouveaux services

Ces 3 premiers services sont présentés dans l'ordre de dépendance.

## Complétion (en français) via Lucene
- adaptation du service actuel /lookup qui utilise SPARQL pur
- changement de l'adresse du service dans le JS (le service sera compatible dbPedia lookup)
## Complétion en tenant compte de la classe RDF
- adaptation du service /lookup
## Utilisation de la Complétion pour l'édition de propriétés objet
Il s'agit d'abandonner l'envoi dans le formulaire de toutes les listes d'URI par classes, par le remplacer par l'appel au service /lookup
## Importation d'un CSV dans un graphe nommé
Voir issue
https://github.com/jmvanel/semantic_forms/issues/77

