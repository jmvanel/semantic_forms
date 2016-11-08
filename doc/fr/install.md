[Installation of the `semantic_forms` generic application](../fr/install.md)

# Installation de l'application générique `semantic_forms`

On parle aussi de "distribution".
Il n'y a pas d'installeur ni d'installation à proprement parler, puisque le contenu zip est prêt à fonctionner.

## Pré-requis 
- Java JRE 8 (vérifier avec `java -version`)

## Obtenir l'application au format .zip
L'application est disponible sous forme de [mise en production github](https://github.com/jmvanel/semantic_forms/releases).

## Démarrer l'Application zippée
Télécharger le zip sur le serveur, dézipper puis taper (sur Linux ou Mac):
```shell
cd semantic_forms_play-1.0-SNAPSHOT
nohup bin/semantic_forms_play -J-Xmx50M &
```

`nohup` démarre l'application de telle façon qu'elle continue de fonctionner après la déconnexion de l'utilisateur.
Autrement, vous pouvez simplement taper:
```
bin/semantic_forms_play -J-Xmx50M &
```
Mais l'application va s'arrêter de fonctionner après la déconnexion de l'utilisateur.

**Sous Windows, il suffit d'exécuter:**
```
bin\semantic_forms_play.bat -J-Xmx50M &
```

Le port par défaut est 9000, donc vous pouvez démarrer votre navigateur à l'adresse suivante [http://localhost:9000](http://localhost:9000) .
L'application générique est parfaitement utilisable telle quelle, voir [User manual](https://github.com/jmvanel/semantic_forms/wiki/User_manual). Cependant, il est préférable de précharger des vocabulaires RDF standards et  les spécifications de formulaires associées et les traductions I18N, pour cela lancez:
```shell
scripts/populateRDFCache.sh
```
**ATTENTION: tous les scripts qui touchent à la base de données doivent être lancés lorsque l'application web est arrêtée.**

Pour plus de détails, voir: [préchargement du contenu RDF](../../scala/forms_play/README.md#preloading-rdf-content) .

#### Arrêter l'application zippée
`kill` l'application java; son identifiant de processus est dans le fichier `RUNNING_PID`.

#### Paramétrage lorsque l'application zippée est en fonctionnement
Vous pouvez changer le port par défaut (9000) vers e.g. 9999 comme suit:

	nohup bin/semantic_forms_play -J-Xmx50M -Dhttp.port=9999 &

Il n'est pas nécessaire de se connecter en tant qu'administrateur.


# Travailler avec la distribution `semantic_forms`

## Scripts pour Unix
Les scripts sont dans le répertoire scripts/ . Il y a actuellement:

```
scripts/clone_implementation.sh
scripts/download-dbpedia.sh
scripts/dump.sh
scripts/graphdump.sh
scripts/graphload.sh
scripts/index_lucene.sh
scripts/load_dump.sh
scripts/populateRDFCache.sh
scripts/start.sh
scripts/stop.sh
scripts/tdbsearch.sh
```

## Configurer une nouvelle instance avec Vocab' communs, spécifications de formulaire, traductions, miroir DBPedia et indexation Lucene
Les scripts doivent être exécutés dans cet ordre:
```
scripts/populate_with_dbpedia.sh
scripts/populateRDFCache.sh
scripts/index_lucene.sh
```

## Sauvegardes et relance à partir des sauvegardes

Les scripts à lancer sont: `dump.sh , clone_implementation.sh , load_dump.sh , start.sh`.
Par exemple, à partir d'un répertoire d'installation `semantic_forms_play-1.0-SNAPSHOT`, ce script sauvegarde les données, puis clone la distribution, puis charge la sauvegarde dans la distribution fraîchement clonée:

```
scripts/dump.sh
scripts/clone_implementation.sh
cd ../semantic_forms_cloned
scripts/load_dump.sh ../semantic_forms_play-1.0-SNAPSHOT
scripts/start.sh
```

But run `scripts/load_dump_no_erase.sh` if you have already RDF content .

