package deductions.runtime

import deductions.runtime.utils.Timer
import org.apache.logging.log4j.LogManager

package object services extends Timer {
  val logger = LogManager.getLogger("services")

  def logTime[T](mess: String, sourceCode: => T): T = {
    super.time(mess, sourceCode, activate = logger.isDebugEnabled())
  }
}