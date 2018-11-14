JENA_DIST=$HOME/apps/apache-jena-3.9.0

echo reload quad dumps dump.nq, dump_timestamps.nq, dump_accounts.nq, using JENA_DIST from $JENA_DIST
ls TDB*
echo this will erase existing databases in TDB* , do want to continue ?
read DUMMY
$JENA_DIST/bin/tdbloader2 --loc TDB dump.nq
$JENA_DIST/bin/tdbloader2 --loc TDB2 dump_timestamps.nq
$JENA_DIST/bin/tdbloader2 --loc TDB3 dump_accounts.nq

