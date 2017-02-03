# Processus
- avant commit, passer systématiquement les tests existants: Selenium, SBT:
```shell
cd ~/src/semantic_forms/scala/forms
sbt test
```
- jamais de régression!
- séparer refactoring et développement fonctionnel dans des commit différents
- au minimum indiquer comment on a testé la nouvelle fonctionnalité avec un URL localhost (voir messages TESTED)
  - développements client-serveur côté client: bien tester le serveur avant
  - développements client-serveur côté serveur: bien indiquer le/les URL de test
  - développements WEB: tests Selenium souhaitables
- pour le code "pur", tests unitaires souhaitables
- liaison issues Github:
  - si on prévoit + de 1 heure de travail, créer une issue d'abord
  - voir Git + bas

# Git
- qualité des messages de commit: point de vue fonctionnalité,
  - ou indiquer "REFACTOR: extract XXX from YYY", ou "REFACTOR: move XXX to YYY", "REFACTOR: rename XXX to YYY", etc
  - ou indiquer "PAVE THE WAY" : préparer le terrain pour une fonctionnalité FFF : cas où on crée une fonction ou classe non encore utilisée
  - pas de "fotes" d'orthographe dans les messages: ce n'est pas bon pour les recherches
- pas + de 2 thèmes pour un commit, surtout si s'il y a plus de 5 fichiers dans le commit
- bien faire une revue des fichiers du commit (pas de scories)
- liaison issues Github: toujours indiquer #numéro "libellé de l'issue";
	si on résoud l'issue, fermer l'issue DANS le commit avec FIX #numéro

# Qualité du code
- pas de copier coller (ou alors REFACTOR ultérieur!)
- formattage: laisser agir l'IDE (2 caractères pour indentations)
- nommage des variables et fonctions
- pas de fonctions longues (+ d'un écran)
- pas de classes longues (+ de 400 lignes)
- le moins possible de variables mutables
- ne pas rattraper les exceptions trop tôt; plutôt utiliser une structure Try[TypeDuRésultat] comme valeur de retour
- private : compartimenter aux maximum le code (il est bien plus facile d'ouvrir ultérieurement que l'inverse)

# Refactoring
Définition: changement qui ne change pas la logique du code ni, donc, les résultats.
- renommage
- déplacement
- extraction de code => variable, => fonction
- changement de la liste de arguments d'une fonction
- l'IDE aide, mais il faut tester!

# Tests
- Selenium
- tests unitaires

# Rapports de bugs
- résultats obtenus
- résultats attendus
- penser à décrire précisément comment reproduire le problème
  - que contient la base?
  - un URL localhost 
