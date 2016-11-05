package views
 
import controllers._
import deductions.runtime.html.MainXml

trait MainXmlWithHead extends MainXml {
  
  /** HTML head */
  override def head(title: String = "")(implicit lang: String = "en") = {
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
