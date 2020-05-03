package deductions.runtime

import com.typesafe.scalalogging.Logger

/** package for Jena specific stuff */
package object jena {
  implicit val logger = Logger("jena")
}