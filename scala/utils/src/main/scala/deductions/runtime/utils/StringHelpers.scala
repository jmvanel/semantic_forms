package deductions.runtime.utils

import java.net.URLEncoder
import deductions.runtime.core.HTTPrequest

trait StringHelpers {

  def substringAfterLastIndexOf(s: String, patt:String): Option[String] = {
    val li = s.lastIndexOf(patt)
    if( li == -1 )
      None
      else
    Some(s.substring( li +1, s.length() ))
  }

  /** introduce Proxy If necessary, that is if URL is HTTP;
   *  does not use proxy if SF server is HTTP . */
  def introduceProxyIfnecessary(url: String,
      request: HTTPrequest = HTTPrequest() ): String =
    if(url.startsWith("http://") && request.secure )
      "/proxy?originalurl=" + URLEncoder.encode(url, "UTF-8")
    else url

}