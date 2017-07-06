package deductions.runtime.sparql_cache.dataset

import java.io.File
import java.net.URL

/** NOTE: Dataset could become a type within RDF, so this would be Rdf#Dataset everywhere */
trait DatasetFactory {
  /** create an In-Memory Dataset */
  def createInMemory: Dataset

  /** create a Dataset backed by a file or directory */
  def createFromFile(file: File): Dataset

  /**
   * create a Dataset from a SPARQL endpoint;
   *  NOTE: this implemtation could be quite generic
   */
  def createFromURL(url: URL): Dataset
}