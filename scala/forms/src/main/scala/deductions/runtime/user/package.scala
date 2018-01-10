package deductions.runtime

import deductions.runtime.utils.Timer
import org.apache.logging.log4j.LogManager

package object user extends Timer {
  val logger = LogManager.getLogger("user")

  def logTime[T](mess: String, sourceCode: => T): T = {
    super.time(mess, sourceCode, activate = logger.isDebugEnabled())
  }
}