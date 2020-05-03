package deductions.runtime.sparql_cache

import com.typesafe.scalalogging.Logger
import deductions.runtime.utils.LogUtils

package object algos extends LogUtils {
  val logger = Logger("algos")

//  def logTime[T](mess: String, sourceCode: => T): T = {
//    super.time(mess, sourceCode, activate = logger.isDebugEnabled())
//  }
}