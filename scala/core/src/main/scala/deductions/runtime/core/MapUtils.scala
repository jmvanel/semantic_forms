package deductions.runtime.core

// import scalaz.Scalaz._
// import scalaz._
// import Scalaz._

trait MapUtils {

  protected def getFirstNonEmptyInMap(
    map: Map[String, Seq[String]],
    uri: String): String = {
    // println(s"getFirstNonEmptyInMap map ${map.mkString(", ")}")
    val uriArgs = map.getOrElse(uri, Seq())
    // println(s"getFirstNonEmptyInMap uriArgs ${uriArgs.mkString(", ")}")
    // uriArgs.find { u => ( u =/= "" ) }.getOrElse("") . trim()
    uriArgs.find { u => ( u != "" ) }.getOrElse("") . trim()
  }

}
