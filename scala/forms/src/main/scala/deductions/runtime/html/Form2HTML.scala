package deductions.runtime.html

import scala.xml.Elem
import deductions.runtime.abstract_syntax.FormModule

/** TODO : different modes: display or edit;
 *  take in account datatype */
trait Form2HTML[URI] extends FormModule[URI] {
  type fm = FormModule[URI]
  def generateHTML(form: fm#FormSyntax[URI]): Elem = {
    <table>
      {
        for (field <- form.fields) yield {
          val l = field.label
          val c = field.comment
          <tr>{
            <td title={ c }>{ l }</td>
            <td>{
              field match {
                case l: LiteralEntry =>
                  <input value={ l.value }></input>
                case r: ResourceEntry =>
                  /* TODO: link to a known resource of the right type,
                   * or create a sub-form for a blank node of an ancillary type (like a street address),
                   * or just create a new resource with its type, given by range, or derived
                   * (like in N3Form in EulerGUI ) */
                  <a href={ r.value.toString }>{ r.label }</a>
              }
            }</td>
          }</tr>
        }
      }
    </table>
  }
}