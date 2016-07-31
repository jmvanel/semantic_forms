# Installation of the `semantic_forms` generic application

## Prerequisites 
- Java JRE 8 (check with `java -version`)

## Obtaining the zipped application
The zipped application is available as a [github release](https://github.com/jmvanel/semantic_forms/releases).

## Runnning the zipped application
Download this zip on the server, unzip and type (on Linux or Mac):
```shell
cd semantic_forms_play-1.0-SNAPSHOT
nohup bin/semantic_forms_play -J-Xmx50M &
```

`nohup` starts the application so that it continues running after the user disconnects.
One can also simply run:
```
bin/semantic_forms_play -J-Xmx50M &
```
but then the application will be stopped when the user disconnects.

**On windows simply run:**
```
bin/semantic_forms_play.bat -J-Xmx50M &
```

The default port is 9000, so you can direct your browser to [http://localhost:9000](http://localhost:9000) .
The generic application is perfectly usable out of the box, see [User manual](https://github.com/jmvanel/semantic_forms/wiki/User_manual). However, it is better to preload common RDF vocabularies and related form specifications and I18N translations, run:
```shell
scripts/populateRDFCache.sh
```
**CAUTION: all scripts involving the database must be run when the web application is stopped.**

For more details, see: [preloading RDF content](../../scala/forms_play/README.md#preloading-rdf-content) .

#### Stopings the zipped distribution
`kill` the java application; its process ID is in the `RUNNING_PID` file.

#### Settings when runnning the zipped distribution
You can change the default port (9000) to e.g. 9999 like this:

	nohup bin/semantic_forms_play -J-Xmx50M -Dhttp.port=9999 &

There is no need to be administrator.
