package views
 
import controllers._
import deductions.runtime.html.MainXml

trait MainXmlWithHead extends MainXml {
  
  /** HTML head */
  override def head(implicit lang: String = "en") = {
    <head>
      <title>{ message("Welcome") }</title>
      <meta http-equiv="Content-type" content="text/html; charset=UTF-8"></meta>
      <link rel="shortcut icon" type="image/png" href={ routes.Assets.at("images/favicon.png").url }/>
      <script src={ routes.Assets.at("javascripts/jquery-1.11.2.min.js").url } type="text/javascript"></script>
      <!-- Latest compiled and minified CSS -->
      <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css"/>
      <link rel="stylesheet" href={ routes.Assets.at("stylesheets/select2.css").url }/>
      <!-- Optional theme -->
      <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap-theme.min.css"/>
      <!-- Latest compiled and minified JavaScript -->
      <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/js/bootstrap.min.js"></script>
      <link href="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0-beta.3/css/select2.min.css" rel="stylesheet"/>
      <script src="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0-beta.3/js/select2.min.js"></script>
      <script src={ routes.Assets.at("javascripts/select2.js").url } type="text/javascript"></script>
      <script src={ routes.Assets.at("javascripts/wikipedia.js").url } type="text/javascript"></script>
      <script src={ routes.Assets.at("javascripts/formInteractions.js").url } type="text/javascript"></script>
      <style type="text/css">
        .resize {{ resize: both; width: 100%; height: 100%; }}
        .overflow {{ overflow: auto; width: 100%; height: 100%; }}
      </style>
    </head>
  }

}