package deductions.runtime.utils

object CommandLineApp extends App {
  println("""
    |List of Applications (See README):
    |================================
    |deductions.runtime.sparql_cache.PopulateRDFCache  load well-know ontologies
    |deductions.runtime.jena.ResetRDFCache             delete named graph related to common vocabularies, default form specifications, and IN18 translations
    |tdb.tdbloader                                     load given RDF file or URL (Jena TDB)
    |tdb.tdbdump                                       dump all RDF in duads (Jena TDB)
    |tdb.tdbupdate                                     SPARQL update (Jena TDB)
    |                                                    DROP GRAPH <$GRAPH>
    |                                                    LOAD <$URI> INTO GRAPH <$GRAPH>
    |tdb.tdbquery                                      SPARQL query (Jena TDB)
    |                                                    SELECT DISTINCT ?g WHERE { GRAPH ?g { ?s ?p ?o } . }
    |                                                    SELECT DISTINCT ?s ?p ?o ?g WHERE { GRAPH ?g { ?s ?p ?o } . OPTIONAL { ?s ?p ?o }
    |                                                      FILTER regex( ?o, '${SEARCH}', 'i') }
    |deductions.runtime.jena.lucene.TextIndexerRDF     index all RDF with Lucene or SORL
    |org.apache.lucene.demo.Se1archFiles               simple text search from lucene-demo
    |
    |deductions.runtime.sparql_cache.RDFI18NLoader     update the I18N translations of the RDF vocabularies
    |deductions.runtime.sparql_cache.FormSpecificationsLoader  update the Common Form Specifications
    |deductions.runtime.jena.DataSourceManagerApp      Replace Same Language triples from given RDF file or URL in given graph
    |deductions.runtime.abstract_syntax.FormSpecificationsFromVocabApp	Create squeleton form specifications from an RDFS/OWL vocabulary
    |
    """.stripMargin
  )
}
