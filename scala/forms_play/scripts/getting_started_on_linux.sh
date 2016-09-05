echo "==== install and run semantic_forms on an empty user account ===="

sudo apt-get install git openjdk-8-jdk-headless

mkdir src apps deploy data
echo "==== install SBT ===="
cd ~/apps
wget https://dl.bintray.com/sbt/native-packages/sbt/0.13.12/sbt-0.13.12.tgz
tar xvzf sbt-0.13.12.tgz
echo 'PATH="$HOME/apps/sbt/bin:$PATH"' >> ~/.bashrc

EMAIL="jeanmarc.vanel@gmail.com"


echo "==== install  key for github ===="
# cf https://github.com/jmvanel/semantic_forms/tree/master/scala/forms_play/#how-to-run
vi ~/.ssh/id_rsa.pub
ssh-keygen -t rsa -b 4096 -C $EMAIL

cd ~/src
git clone git@github.com:jmvanel/semantic_forms.git

echo "==== build banana-rdf from sources ===="
git clone git@github.com:deductions/banana-rdf.git
cd banana-rdf/
sbt publishLocal

echo "==== build semantic_forms from sources ===="
echo "See https://github.com/jmvanel/semantic_forms/tree/master/scala/forms_play/#how-to-run"
cd ~/src/semantic_forms/scala/forms_play
echo type Control-D to stop the server
sbt run

echo "==== load content into TDB: basic ontologies and forms; persons, dbPedia, ===="
sbt "runMain deductions.runtime.sparql_cache.PopulateRDFCache"

echo browse http://localhost:9000/display?displayuri=http%3A%2F%2Fjmvanel.free.fr%2Fjmv.rdf%23me&Display=Afficher

