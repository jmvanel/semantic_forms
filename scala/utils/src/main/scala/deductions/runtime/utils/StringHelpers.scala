package deductions.runtime.utils

import java.net.URLEncoder

trait StringHelpers {

  def substringAfterLastIndexOf(s: String, patt:String): Option[String] = {
    val li = s.lastIndexOf(patt)
    if( li == -1 )
      None
      else
    Some(s.substring( li +1, s.length() ))
  }

  def introduceProxyIfnecessary(url: String): String =
    if(url.startsWith("http://"))
      "/proxy?originalurl=" + URLEncoder.encode(url, "UTF-8")
    else url

}