package deductions.runtime.services

import deductions.runtime.utils.Configuration
/**
 * tested with
 * wget --method=OPTIONS --save-headers http://localhost:9000/bla
 *
 * cf http://www.html5rocks.com/en/tutorials/cors/#toc-adding-cors-support-to-the-server
 *  https://developer.mozilla.org/fr/docs/HTTP/Access_control_CORS
 */
trait CORS //extends Configuration
{
  val config: Configuration
  import config._

  // currently PUT is not implemented for LDP
  val corsHeaders: Map[String, String] = Map(
    "Access-Control-Allow-Origin" -> allow_Origin,
    "Access-Control-Allow-Methods" -> "GET, POST",
    "Access-Control-Allow-Headers" -> "Slug, Link, Origin, Content-Type, Accept",
    // removed, nothing to do with CORS :   ActorSystemImpl - Explicitly set HTTP header 'Content-Type: text/turtle; charset=utf-8, application/ld+json; charset=utf-8' is ignored, illegal RawHeader
    // "Content-Type" -> "text/turtle; charset=utf-8, application/ld+json; charset=utf-8",

    "Allow" -> "OPTIONS,HEAD,GET,POST,PUT,PATCH",
    "Accept-Post" -> "text/turtle, application/ld+json",
    "Accept-Patch" -> "text/ldpatch",
    "Link" -> """<http://www.w3.org/ns/ldp#BasicContainer>; rel="type", <http://www.w3.org/ns/ldp#Resource>; rel="type""""
  )
}