package deductions.runtime

import deductions.runtime.utils.Timer
import org.apache.logging.log4j.LogManager

package object rdf_links_rank extends Timer {
  val logger = LogManager.getLogger("rdf_links_rank")

  def logTime[T](mess: String, sourceCode: => T): T = {
    super.time(mess, sourceCode, activate = logger.isDebugEnabled())
  }
}