package deductions.runtime

//import org.apache.logging.log4j.LogManager
//import org.apache.log4j.Logger

package object utils extends Timer {
//  val logger = LogManager.getLogger("services")
//
//  def logTime[T](mess: String, sourceCode: => T): T = {
//    super.time(mess, sourceCode, activate = logger.isDebugEnabled())
//  }
  val logActive = false
}