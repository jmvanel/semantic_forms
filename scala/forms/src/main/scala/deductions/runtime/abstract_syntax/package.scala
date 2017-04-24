package deductions.runtime

import org.apache.logging.log4j.LogManager
//import deductions.runtime.utils.Timer

package object abstract_syntax
//extends Timer
{
  val logger = LogManager.getLogger("abstract_syntax")

//  def time[T](mess: String, sourceCode: => T): T = {
//    super.time(mess, sourceCode, activate = logger.isDebugEnabled())
//  }

}