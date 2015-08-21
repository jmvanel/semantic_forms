package deductions.runtime.utils

/**
 * measure any code fragment
 * @author jmv
 */
trait Timer {

  def time[T](mess: String, sourceCode: => T): T = {
    val start = System.currentTimeMillis()
    val res = sourceCode
    val end = System.currentTimeMillis()
    println(s"""Time elapsed: "$mess": ${end - start}ms""")
    res
  }
}