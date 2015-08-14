package deductions.runtime.utils

import scala.io.Source

/**
 * @author jmv
 */

/**
 * Fix Bad URI in N-Quads dump:
 *  From:
 *  <bad URI> <p> <bad Object> .
 *  To:
 *  <bad_URI> <p> <bad_Object> .
 */
object FixBadURI extends App {
  var corrections = 0
  for (line <- Source.fromFile("dump.nq").getLines()) {
    val parts = line.split(" +")
    val subjet = parts(0)
    val predicate = parts(1)
    val objet = parts(2)
    val graph = parts(3)
    if (subjet.contains(" ") ||
      (objet.startsWith("<") &&
        objet.contains(" "))) {
      System.err.println("# " + line)
      corrections = corrections + 1
    }
    val underscore = "_"
    println(
      subjet.replaceAll(" ", underscore) + " " +
      predicate + " " +
        (if (objet.startsWith("<"))
          objet.replaceAll(" ", underscore)
        else objet)
      +" "+ graph)
  }
  System.err.println( "" + corrections + " corrections" ) 

}