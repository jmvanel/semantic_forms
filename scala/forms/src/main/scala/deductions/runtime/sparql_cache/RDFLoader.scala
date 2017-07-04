package org.w3.banana.io

import org.w3.banana.RDF

import scala.language.higherKinds

/**
 * RDF loader
 *  >>>> temporarily added this, pending Banana RDf pull request
 */
trait RDFLoader[Rdf <: RDF, M[_]] {

  /**
   * Read triples from the given location. The syntax is determined from input source URI
   *  (content negotiation or extension).
   */
  def load(url: java.net.URL): M[Rdf#Graph]

}
