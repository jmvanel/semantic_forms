package deductions.runtime

import com.typesafe.scalalogging.Logger


package object utils extends Timer {
  val logger = Logger("utils")

  val logActive = false
  def println1(mess: String) = if (logActive) println(mess)
}