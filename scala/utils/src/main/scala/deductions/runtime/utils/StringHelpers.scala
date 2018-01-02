package deductions.runtime.utils

trait StringHelpers {

  def substringAfterLastIndexOf(s: String, patt:String): Option[String] = {
    val li = s.lastIndexOf(patt)
    if( li == -1 )
      None
      else
    Some(s.substring( li +1, s.length() ))
  }
}