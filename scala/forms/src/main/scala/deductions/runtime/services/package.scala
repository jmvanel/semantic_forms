package deductions.runtime

import org.apache.logging.log4j.LogManager
import org.apache.log4j.Logger

 import deductions.runtime.utils.Timer

package object services extends Timer {
  val logger = LogManager.getLogger("services")

  def logTime[T](mess: String, sourceCode: => T): T = {
    super.time(mess, sourceCode, activate = logger.isDebugEnabled())
  }
}