package deductions.runtime.html

trait CSS {

  lazy val cssClasses = tableCSSClasses

  val cssRules = """
      input.form-control[type='text']{ margin-bottom: 15px; }
      /*.form-root{ margin-left: 10px; }
      .form-row{ display: table-row; }
      .form-cell{ display: table-cell; }
      .form-label{ display: table-cell; width: 160px;
                   border-collapse: separate;
                   border-width: 10px;
                   background-color: lightcoral; }
      .form-input{ display: table-cell; width: 500px;
                   border-collapse: separate;
                   border-width: 2px;}
      .form-value{ display: table-cell; width: 500px;
                   border-collapse: separate;
                   border-width: 10px; }
      .button-add{ width: 25px; }*/
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
    val formSelectCSSClass: String = "form-control")

  /** actually applied CSS classes are here */
  lazy val tableCSSClasses = CSSClasses(
    formRootCSSClass = "form-horizontal",
    formFieldCSSClass = "",
    formLabelAndInputCSSClass = "form-group",
    formLabelCSSClass = "control-label col-xs-1",
    formDivInputCSSClass = "col-xs-9",
    formInputCSSClass = "form-control",
    formDivEditInputCSSClass = "col-xs-1",
    formAddDivCSSClass = "col-xs-1",
    formSelectDivCSSClass = "col-xs-1",
    formSelectCSSClass = "form-control")
}
