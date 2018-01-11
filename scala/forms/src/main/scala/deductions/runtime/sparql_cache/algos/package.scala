package deductions.runtime.sparql_cache

import deductions.runtime.utils.Timer
import org.apache.logging.log4j.LogManager

package object algos extends Timer {
  val logger = LogManager.getLogger("algos")

  def logTime[T](mess: String, sourceCode: => T): T = {
    super.time(mess, sourceCode, activate = logger.isDebugEnabled())
  }
}