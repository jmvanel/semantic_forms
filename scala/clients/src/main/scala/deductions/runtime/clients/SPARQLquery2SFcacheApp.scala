package deductions.runtime.clients

import org.w3.banana.jena.JenaModule

object SPARQLquery2SFcacheApp
  extends JenaModule
  with SPARQLquery2SFcache with App {
  val query = args(0)
  val endpoint = args(1)
  val sfInstancePrefix = args(2)
  
  importFromQuery(query, endpoint, sfInstancePrefix)
}