package deductions.runtime.clients

import org.w3.banana.jena.JenaModule

/** From a SPARQL query to another endpoint,
 * load URI's returned by the query into SF;
 * in the end it is like if a user had pasted all those URI's
 * and clicked on "Display".
 * It amounts to make linked copies of all these semantic URI's into SF;
 *
 * Run eg with:
 *
 * <pre>
   runMain deductions.runtime.clients.SPARQLquery2SFcacheApp
    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> 
    SELECT * 
    WHERE {
     ?sub rdfs:subClassOf* <http://taxref.mnhn.fr/lod/taxon/185292/10.0> .
      ?sub rdfs:label ?LAB .  ?sub <http://taxref.mnhn.fr/lod/property/hasRank> ?RANK .
    } LIMIT 5"
       http://taxref.mnhn.fr/sparql
       http://localhost:9000
   </pre>
   
   * (all on one ligne because of SBT )
   * 
   * CAUTION: main variable MUST be ?sub */
object SPARQLquery2SFcacheApp
  extends JenaModule
  with SPARQLquery2SFcache with App {
  val query = args(0)
  val endpoint = args(1)
  val sfInstancePrefix = args(2)
  
  importFromQuery(query, endpoint, sfInstancePrefix)
}