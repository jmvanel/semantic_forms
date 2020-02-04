package controllers

/** main Application controller */
object Application extends {
    override implicit val config = new PlayDefaultConfiguration
  }
  // with WebPages
  with SparqlServices
  with HTMLGenerator
