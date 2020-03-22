package deductions.runtime.jena

import org.apache.logging.log4j.LogManager

/** package for Jena + Lucene specific stuff */
package object lucene {
  implicit val logger = LogManager.getLogger("jena")
}