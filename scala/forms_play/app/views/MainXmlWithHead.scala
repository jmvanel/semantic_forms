package views
 
import scala.io.Source
import scala.xml.Elem
import scala.xml.Node
import scala.xml.XML

import controllers.routes
import deductions.runtime.html.MainXml

trait MainXmlWithHead extends MainXml {

  private def add(e: Elem, c: Node): Elem = e.copy(child = e.child ++ c)
  private val basicHead = XML.loadString(
    Source.fromURL(getClass.getResource("/deductions/runtime/html/head.html")).getLines().mkString("\n"))

  /** HTML head */
  override def head(title: String = "")(implicit lang: String = "en"): Elem = {
    val titleTag =
      <title>
        {
          val default = messageI18N("Welcome")
          if (title != "")
            s"$title - $default"
          else
            default
        }
      </title>
    add(basicHead, titleTag)
  }
  
  private def headOLD(title: String = "")(implicit lang: String = "en") = {
    <head>
      <title>{
        val default = messageI18N("Welcome")
        if( title != "")
          s"$title - $default"
        else
          default }
      </title>

      <meta http-equiv="Content-type" content="text/html; charset=UTF-8"></meta>
      <link rel="shortcut icon" type="image/png" href={ routes.Assets.at("images/favicon.png").url }/>

      { javascriptCSSImports }
      
      <style type="text/css">
        .resize {{ resize: both; width: 100%; height: 100%; }}
        .overflow {{ overflow: auto; width: 100%; height: 100%; }}
				.unselectable {{
  user-select: none;
  -moz-user-select: none;
  -webkit-user-select: none;
  -ms-user-select: none;
}}
.selectable {{
  user-select: text;
  -moz-user-select: text;
  -webkit-user-select: text;
  -ms-user-select: text;
}}
      </style>
    </head>
  }

}
