package deductions.runtime.services

/** tested with
 * wget --method=OPTIONS --save-headers http://localhost:9000/bla
 * 
 * cf http://www.html5rocks.com/en/tutorials/cors/#toc-adding-cors-support-to-the-server
 *  https://developer.mozilla.org/fr/docs/HTTP/Access_control_CORS
 */
trait CORS extends Configuration {
  // currently PUT is not implemented for LDP
  val corsHeaders: Map[String, String] = Map(
    "Access-Control-Allow-Origin" -> allow_Origin,
    "Access-Control-Allow-Methods" -> "GET, POST",
    "Access-Control-Allow-Headers" -> "Slug, Link, Origin, Content-Type, Accept",
    "Content-Type" -> "text/turtle; charset=utf-8, text/json-ld; charset=utf-8"
  )
}