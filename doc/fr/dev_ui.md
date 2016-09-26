Développements IHM, CSS, et JS
==============================

# RDFViewer
**But**
Issues 
[Diagram (graph drawing) enhancements #91](https://github.com/jmvanel/semantic_forms/issues/91)

# CSS formulaires

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

# Développements d'applications utilisant l'API Web /form

**But**
Voir comment utiliser l'API /form pour écrire une petite application simple.

# Exemple 1: suggestions de musiques et vote
- des danseurs se voient proposer des styles et/ou des morceaux; ils votent ( cochent ) ce qu 'ils veulent
- une autre page montre le résultat du vote

La page 2 va utiliser une requête SPARQL qui renvoie un résultat en JSON-LD.

# Exemple 2: suggestions d'objets proches
- une page renvoie les objets proches d'un objet donné; les résultats sont objets prochesclassés par types.

La page va utiliser une requête SPARQL qui renvoie un résultat en JSON-LD.
