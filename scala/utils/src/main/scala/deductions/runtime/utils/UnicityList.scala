package deductions.runtime.utils

import scala.collection.mutable

/** List without duplication of element, using a Set */
case class UnicityList[E]() {
  private val liste = mutable.ArrayBuffer[E]()
  private val set = mutable.Set[E]()

  def add(e: E) =
    if (set.add(e)) {
      liste += e
    }

  def list = liste.toIterable
}