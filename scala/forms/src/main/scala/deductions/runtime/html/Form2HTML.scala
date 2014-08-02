package deductions.runtime.html

import scala.xml.Elem
import deductions.runtime.abstract_syntax.FormModule
import java.net.URLEncoder

/** TODO : different modes: display or edit;
 *  take in account datatype */
trait Form2HTML[NODE, URI] extends FormModule[NODE, URI] {
  type fm = FormModule[NODE, URI]
  def generateHTML(form: fm#FormSyntax[NODE, URI],
      hrefPrefix:String=""): Elem = {
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
                  <a href={ createHyperlinkString( hrefPrefix, r.value.toString) }>{
                    r.value.toString
                  }</a>
                case r: BlankNodeEntry =>
                  <a href={ createHyperlinkString( hrefPrefix, r.value.toString, "&blanknode=true") }>{
                    r.getId // value.toString
                  }</a>
              }
            }</td>
          }</tr>
        }
      }
    </table>
  }
  
  def createHyperlinkString( hrefPrefix:String, uri:String, suffix:String="" ) :String = {
    if ( hrefPrefix == "" )
      uri
    else
      hrefPrefix + URLEncoder.encode(uri, "utf-8") + suffix
  }
}