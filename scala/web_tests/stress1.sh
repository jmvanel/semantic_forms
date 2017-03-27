 for i in {1..100};
do 
echo $i;
  wget http://localhost:9000/display?displayuri=dbpedia%3AParadox
  echo RETURN_CODE $?
done

