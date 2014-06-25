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
                  <input>{ l.value }</input>
                case r: ResourceEntry =>
                  <a href={ r.value.toString }>{ r.label }</a>
              }
            }</td>
          }</tr>
        }
      }
    </table>
  }
}