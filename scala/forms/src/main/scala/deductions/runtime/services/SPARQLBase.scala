package deductions.runtime.services

trait SPARQLBase {

  val countPattern =
    """|  OPTIONAL {
       |   graph ?grCount {
       |    ?thing form:linksCount ?COUNT.
       |  } }""".stripMargin

}