package deductions.runtime

import deductions.runtime.utils.Timer
//import org.apache.logging.log4j.LogManager
import com.typesafe.scalalogging.Logger

package object services extends Timer {
//  val logger = LogManager.getLogger("services")
  // migrate to scala-logging
  val logger = Logger("server")

  def isDebugEnabled(logger :Logger) = {
    var isDebugEnabled = false
    logger.whenDebugEnabled{
      isDebugEnabled = true
    }
    isDebugEnabled
  }

  /** execute source Code and mesure CPU time, when Debug Enabled */
  def logTime[T](mess: String, sourceCode: => T): T = {
    super.time(mess, sourceCode, activate = isDebugEnabled(logger) )
  }
}