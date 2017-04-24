package deductions.runtime.utils

trait Maps {

  type mapsOfLists[KEYS, ITEMS] = Map[KEYS, Seq[ITEMS]]

  /**merge 2 Maps Of Lists, list values are concatenated for each key */
  def mergeMapsOfLists[KEYS, ITEMS](m1: mapsOfLists[KEYS, ITEMS], m2: mapsOfLists[KEYS, ITEMS]): mapsOfLists[KEYS, ITEMS] = {
    for (
      (key1, list1) <- m1;
      key1ItemsInM2 = m2.getOrElse(key1, List())
    ) yield {
      (key1, list1 ++ key1ItemsInM2)
    }
  }
}