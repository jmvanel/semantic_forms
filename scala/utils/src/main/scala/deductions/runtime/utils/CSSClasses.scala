package deductions.runtime.utils

/** CSS for forms */
trait CSSClasses {

  lazy val cssClasses = tableCSSClasses

  val cssRules = """
      input.form-control[type='text']
      .form-value{ display: table-cell; width: 500px;
                   border-collapse: separate;
                   border-width: 10px; }
      .sf-external-link{color: DarkRed; background-color: lightyellow;}
      .sf-internal-link{}
      .sf-sparql-table td { padding:0px 10px; }
      .sf-sparql-table tr:first-of-type { font-weight:bold; }
"""

  lazy val localCSS = <style type='text/css'>{ cssRules }</style>

  /** default is here */
  case class CSSClasses(
    val formRootCSSClass: String = "form",
    val formFieldCSSClass: String = "form-group",
    val formLabelAndInputCSSClass: String = "row",
    val formLabelCSSClass: String = "control-label",
    val formDivInputCSSClass: String = "col-xs-9",
    val formInputCSSClass: String = "input",
    val formDivEditInputCSSClass: String = "col-xs-1",
    val formAddDivCSSClass: String = "col-xs-1",
    val formSelectDivCSSClass: String = "col-xs-1",
    val formSelectCSSClass: String = "form-select")

  /** table CSS Classes (Bootstrap), actually applied CSS classes are here */
  lazy val tableCSSClasses = CSSClasses(
    // See https://getbootstrap.com/docs/3.3/css/#forms-horizontal
    formRootCSSClass = "form-horizontal",

    formFieldCSSClass = "",
    formLabelAndInputCSSClass = "form-group",
    formLabelCSSClass = "col-xs-3 col-sm-2 col-md-2 control-label",
//    formLabelCSSClass = "control-label",
//    formLabelCSSClass = "control-label",
    formDivInputCSSClass = "col-xs-8",
    formInputCSSClass = "form-control",
    formDivEditInputCSSClass = "col-xs-4",
    formAddDivCSSClass = "col-xs-1 col-sm-1 col-md-1",
    formSelectDivCSSClass = "col-xs-4",
    formSelectCSSClass = "form-control form-select")

  /** TODO use the LDP prefix; distinguish 1) dbpedia URI's 2) other external links 3) internal links */
  def cssForURI(uri: String) = {
    // if (uri.startsWith("http://dbpedia.org/resource/"))
    "sf-rdf-link " + (
    if (uri.contains("dbpedia.org/resource/"))
      "sf-external-link"
    else
      "sf-internal-link" )
  }
}
