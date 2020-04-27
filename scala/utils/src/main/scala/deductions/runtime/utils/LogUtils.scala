package deductions.runtime.utils

import com.typesafe.scalalogging.Logger
//import deductions.runtime.utils.Timer

trait LogUtils extends Timer {

  def isDebugEnabled(logger :Logger) = {
    var isDebugEnabled = false
    logger.whenDebugEnabled{
      isDebugEnabled = true
    }
    isDebugEnabled
  }

  /** execute source Code and mesure CPU time, when Debug Enabled */
  def logTime[T](mess: String, sourceCode: => T)(implicit logger :Logger): T = {
    super.time(mess, sourceCode, activate = isDebugEnabled(logger) )
  }
}