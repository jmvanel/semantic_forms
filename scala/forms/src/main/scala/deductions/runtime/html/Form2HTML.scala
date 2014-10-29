package deductions.runtime.html

import scala.xml.Elem
import deductions.runtime.abstract_syntax.FormModule
import java.net.URLEncoder

import Form2HTML._

/** different modes: display or edit;
 *  take in account datatype */
trait Form2HTML[NODE, URI<: NODE] extends FormModule[NODE, URI] {
  type fm = FormModule[NODE, URI]
  def generateHTML(form: fm#FormSyntax[NODE, URI],
      hrefPrefix:String="",
      editable:Boolean=false,
      actionURI:String="/save"): Elem = {
        
    val htmlForm = 
    <table>
    <input type="hidden" name="uri" value={urlEncode(form.subject)}/>
      {
        for (field <- form.fields) yield {
          val l = field.label
          val c = field.comment
          <tr>{
            <td title={ c }>{ l }</td>
            <td>{
              field match {
                case l: LiteralEntry =>
                  if( editable)
                  <input value={ l.value } name={"LIT-"+urlEncode(l.property)} style="resize: both" />
                  <input value={ l.value } name={"ORIG-LIT-"+urlEncode(l.property)} type="hidden" />
                  else
                  <div>{ l.value }</div>
                case r: ResourceEntry =>
                  /* link to a known resource of the right type,
                   * or create a sub-form for a blank node of an ancillary type (like a street address),
                   * or just create a new resource with its type, given by range, or derived
                   * (like in N3Form in EulerGUI ) */
                  if( editable )
                  <input value={ r.value.toString } name={"RES-"+urlEncode(r.property)} style="resize: both" />
                  <input value={ r.value.toString } name={"ORIG-RES-"+urlEncode(r.property)} type="hidden" />
                  else
                  <a href={ Form2HTML.createHyperlinkString( hrefPrefix, r.value.toString) }>{
                    r.value.toString
                  }</a>
                case r: BlankNodeEntry =>
                  if( editable )
                  <input value={ r.value.toString } name={"BLA-"+urlEncode(r.property)} style="resize: both" />
                  <input value={ r.value.toString } name={"ORIG-BLA-"+urlEncode(r.property)} type="hidden" />

                  else
                  <a href={ Form2HTML.createHyperlinkString( hrefPrefix, r.value.toString, true) }>{
                    r.getId
                  }</a>
              }
            }</td>
          }</tr>
        }
      }
    </table>
      if( editable)
        <form action={actionURI}  method="POST" >
          <input value="SAVE" type="submit"/>
          <input type="hidden" name="url" value={urlEncode(form.subject)}/>
          {htmlForm}
        </form>
        else
          htmlForm
  }
}

object Form2HTML {
  def urlEncode(node:Any) = {URLEncoder.encode( node.toString, "utf-8")}

  def createHyperlinkString( hrefPrefix:String, uri:String, blanknode:Boolean=false ) :String = {
    if ( hrefPrefix == "" )
      uri
    else {
      val suffix = if(blanknode) "&blanknode=true" else ""
      hrefPrefix + urlEncode(uri) + suffix
    }
  }
}