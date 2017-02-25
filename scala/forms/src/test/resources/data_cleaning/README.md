- merge1.* : pure renaming of rdfs:label specified by CSV file
	```shell
	java -$JARS deductions.runtime.data_cleaning.DuplicateCleanerSpecificationApp \
		merge1.csv merge1.ttl
	```
- merge2.* : merge by rdfs:label; differences in french accents are considered as good for merging
	```shell
	java -$JARS deductions.runtime.data_cleaning.DuplicateCleanerFileApp \
		http://www.w3.org/2002/07/owl#Class
		src/test/resources/data_cleaning/merge2.ttl
	```



