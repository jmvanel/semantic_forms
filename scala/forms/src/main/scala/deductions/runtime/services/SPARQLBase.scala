package deductions.runtime.services

trait SPARQLBase {

  val countPattern =
    """|  OPTIONAL {
       |   graph ?grCount {
       |    ?thing form:linksCount ?COUNT.
       |  } }""".stripMargin

  private val excludePersonOLD = """  FILTER( NOT EXISTS { ?thing a dbo:Person } )"""
  
  /** NOTE designed for <urn:x-arq:UnionGraph> */
  val excludePerson = """  FILTER( NOT EXISTS { ?thing a ?sub . ?sub rdfs:subClassOf* dbo:Person } )"""
  val excludePlace  = """  FILTER( NOT EXISTS { ?thing a ?sub . ?sub rdfs:subClassOf* dbo:Place } )"""
}