package deductions.runtime.html

trait CSS {

  lazy val cssClasses = tableCSSClasses

  val cssRules = """
      .form-root{ margin-left: 10px; }
      .form-row{ display: table-row; }
      .form-cell{ display: table-cell; }
      .form-label{ display: table-cell; width: 160px;
                   border-collapse: separate;
                   border-width: 10px;
                   background-color: lightcoral; }
      .form-input{ display: table-cell; width: 500px;
                   border-collapse: separate;
                   border-width: 10px;}
      .form-value{ display: table-cell; width: 500px;
                   border-collapse: separate;
                   border-width: 10px; }
      .button-add{ width: 25px; }
"""

  lazy val localCSS = <style type='text/css'>{ cssRules }</style>

  /** default is here */
  case class CSSClasses(
    val formRootCSSClass: String = "form",
    val formFieldCSSClass: String = "form-group",
    val formLabelAndInputCSSClass: String = "row",
    val formLabelCSSClass: String = "control-label",
    val formInputCSSClass: String = "input")

  /** actually applied CSS classes are here */
  lazy val tableCSSClasses = CSSClasses(
    formRootCSSClass = "form-root",
    formFieldCSSClass = "",
    formLabelAndInputCSSClass = "form-row",
    formLabelCSSClass = "form-label",
    formInputCSSClass = "form-input")
}
