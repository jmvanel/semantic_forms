echo "==== install and run semantic_forms on an empty user account ===="

EMAIL="jeanmarc.vanel@gmail.com"

mkdir src apps deploy data

echo "==== install  key for github ===="
# cf https://github.com/jmvanel/semantic_forms/tree/master/scala/forms_play/#how-to-run
vi ~/.ssh/id_rsa.pub
ssh-keygen -t rsa -b 4096 -C $EMAIL

cd src
git clone git@github.com:jmvanel/semantic_forms.git
cd

echo "==== install  activator (SBT like ) ===="
wget http://downloads.typesafe.com/typesafe-activator/1.3.4/typesafe-activator-1.3.4-minimal.zip?_ga=1.44982880.594876161.1414070394
unzip types*
mv activator-1.3.4-minimal/  apps/

echo "==== build semantic_forms from sources ===="
echo "See https://github.com/jmvanel/semantic_forms/tree/master/scala/forms_play/#how-to-run"
cd ~/src/semantic_forms/scala/forms_play
echo type Control-D to stop the server
$HOME/apps/activator-1.3.4-minimal/activator run

echo "==== load content into TDB: basic ontologies and forms; persons, dbPedia, ===="
$HOME/apps/activator-1.3.4-minimal/activator \
  "runMain deductions.runtime.sparql_cache.PopulateRDFCache"

echo browse http://localhost:9000/display?displayuri=http%3A%2F%2Fjmvanel.free.fr%2Fjmv.rdf%23me&Display=Afficher

