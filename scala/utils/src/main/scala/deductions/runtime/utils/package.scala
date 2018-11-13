package deductions.runtime

//import org.apache.logging.log4j.LogManager
//import org.apache.log4j.Logger

package object utils extends Timer {
//  val logger = LogManager.getLogger("utils")
//
//  def logTime[T](mess: String, sourceCode: => T): T = {
//    super.time(mess, sourceCode, activate = logger.isDebugEnabled())
//  }
  val logActive = false
  def println1(mess: String) = if (logActive) println(mess)
}