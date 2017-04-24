package deductions.runtime.utils

/**
 * measure any code fragment
 * @author jmv
 */
trait Timer {

  /** wrap the block of code and returns the result */
  def time[T](mess: String, sourceCode: => T, activate: Boolean = false): T = {
    if (activate) {
      val start = System.currentTimeMillis()
      val res = sourceCode
      val end = System.currentTimeMillis()
      System.err.println(s"""Time elapsed: "$mess": ${end - start}ms""")
      System.err.flush()
      res
    } else sourceCode
  }
  
//  def time[T](mess: String, sourceCode: => T)(implicit logger: Logger): T = {
//    time(mess, sourceCode, activate = logger.isDebugEnabled())
//  }
}

object Timer {
  private var start: Long = 0
  def startTimer() = start = System.currentTimeMillis()
  def endTimer(mess: String = "Timer") = {
    val end = System.currentTimeMillis()
    println(s"""Time elapsed: "$mess": ${end - start}ms""")
  }
}