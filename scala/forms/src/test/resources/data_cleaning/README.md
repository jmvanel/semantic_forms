- merge1.* : pure renaming of rdfs:label specified by CSV file
	```shell
	t=src/test/resources/data_cleaning
	java -cp $JARS deductions.runtime.data_cleaning.DuplicateCleanerSpecificationApp \
		$t/merge1.csv $t/merge1.ttl
	```
- merge2.* : merge by rdfs:label; differences in french accents are considered as good for merging
	```shell
	t=src/test/resources/data_cleaning
	java -cp $JARS deductions.runtime.data_cleaning.DuplicateCleanerFileApp \
		http://www.w3.org/2002/07/owl#Class
		$t/merge2.ttl
	```

- merge3.* : merge by specification
	```shell
	t=src/test/resources/data_cleaning
	java -cp $JARS deductions.runtime.data_cleaning.DuplicateCleanerSpecificationApp \
		$t/merge3.csv $t/merge3.ttl
	```

- merge4.* : merge object properties by label and rdfs:range
	```shell
	t=src/test/resources/data_cleaning
	java -cp $JARS deductions.runtime.data_cleaning.DuplicateCleanerFileApp \
	http://www.w3.org/2002/07/owl#ObjectProperty $t/merge3.csv.ttl
	```


