# Installation de l'application générique `semantic_forms`

## Pré-requis 
- Java JRE 8 (vérifier avec `java -version`)

## Obtenir l'application au format .zip
L'application est disponible sous forme de  [github release](https://github.com/jmvanel/semantic_forms/releases).

## Démarrer l'Application zippée
Télécharger le zip sur le serveur, dézipper puis taper (on Linux or Mac):
```shell
cd semantic_forms_play-1.0-SNAPSHOT
nohup bin/semantic_forms_play -J-Xmx50M &
```

`nohup` démarre l'application donc elle continue de fonctionner après la déconnexion de l'utilisateur.
Autrement, vous pouvez simplement taper:
```
bin/semantic_forms_play -J-Xmx50M &
```
Mais l'application va s'arrêter de fonctionner après la déconnexion de l'utilisateur.

**Sous Windows, il suffit d'exécuter:**
```
bin/semantic_forms_play.bat -J-Xmx50M &
```

Le port par défaut est 9000, donc vous pouvez démarrer votre navigateur à l'adresse suivante [http://localhost:9000](http://localhost:9000) .
L'application générique est parfaitement utilisable ainsi, voir [User manual](https://github.com/jmvanel/semantic_forms/wiki/User_manual). Cependant, il est préférable de précharger des vocabulaires RDF standards et relatifs aux spécifications et les traductions I18N, lancez:
```shell
scripts/populateRDFCache.sh
```
**ATTENTION: tous les scripts qui touchent à la base de données doivent être lancés lorsque l'application web est arrêtée.**

Pour plus de détails, voir: [préchargement du contenu RDF](../../scala/forms_play/README.md#preloading-rdf-content) .

#### Arrêter l'application zippée
`kill` l'application java; son ID process est dans le fichier `RUNNING_PID`.

#### Paramétrage lorsque l'application zippée est en fonctionnement
Vous pouvez changer le port par défaut (9000) vers e.g. 9999 comme suit:

	nohup bin/semantic_forms_play -J-Xmx50M -Dhttp.port=9999 &

Il n'est pas nécessaire de se connecter en tant qu'administrateur.
