package deductions.runtime.core

import scalaz.Scalaz._

trait MapUtils {

  protected def getFirstNonEmptyInMap(
    map: Map[String, Seq[String]],
    uri: String): String = {
    val uriArgs = map.getOrElse(uri, Seq())
    uriArgs.find { uri => uri  =/=  "" }.getOrElse("") . trim()
  }

}