package deductions.runtime.utils

import scala.collection.mutable

/** List without duplication of element, using a Set */
case class UnicityList[E](list0: Iterable[E] = mutable.ArrayBuffer[E]()) {
  private val liste = mutable.ArrayBuffer[E]()
  liste ++= list0
  private val set = mutable.Set[E]()
  set ++= list0

  def add(e: E) =
    if (set.add(e)) {
      liste += e
    }

  def list = liste.toIterable
}