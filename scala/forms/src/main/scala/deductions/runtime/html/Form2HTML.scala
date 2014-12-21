package deductions.runtime.html

import scala.xml.Elem
import deductions.runtime.abstract_syntax.FormModule
import java.net.URLEncoder

import Form2HTML._

/**
 * different modes: display or edit;
 *  take in account datatype
 */
trait Form2HTML[NODE, URI <: NODE] extends FormModule[NODE, URI] {
  type fm = FormModule[NODE, URI]
  def generateHTML(form: fm#FormSyntax[NODE, URI],
    hrefPrefix: String = "",
    editable: Boolean = false,
    actionURI: String = "/save"): Elem = {

    val htmlForm =
      <div class="container">
        <input type="hidden" name="uri" value={ urlEncode(form.subject) }/>
        <div class="form">
          {
            for (field <- form.fields) yield {
            <div class="form-group">
              <div class="row">
                <label class="col-md-4 control-label" title={ field.comment }>{ field.label }</label>
                <div class="col-md-8">
              		{
                    createHTMLField(field, editable, hrefPrefix)
                  }
								</div>
						  </div>
						</div>  
            }
          }
    	</div>
		</div>
    
    if( editable)
      <form action={actionURI}  method="POST">
					<p class="text-right">
						<input value="SAVE" type="submit" class="btn btn-primary btn-lg" />
					</p>
          <input type="hidden" name="url" value={urlEncode(form.subject)}/>
          {htmlForm}
          <p class="text-right">
          		<input value="SAVE" type="submit" class="btn btn-primary btn-lg pull-right" />
					</p>
        </form>
      else
        htmlForm
  }         
            
  def createHTMLField(field:fm#Entry, editable:Boolean,
          hrefPrefix: String = "" ) = {
      field match {
        case l: LiteralEntry =>
            		{if (editable) {
									<input class="form-control" value={ l.value } name={ "LIT-" + urlEncode(l.property) } />
            			<input value={ l.value } name={ "ORIG-LIT-" + urlEncode(l.property) } type="hidden"/>
                } else {
            			<div>{ l.value }</div>
                }}
        case r: ResourceEntry =>
          /* link to a known resource of the right type,
           * or create a sub-form for a blank node of an ancillary type (like a street address),
           * or just create a new resource with its type, given by range, or derived
           * (like in N3Form in EulerGUI ) */
          {if (editable) {
              <div>
          			<input class="form-control" value={ r.value.toString } name={ "RES-" + urlEncode(r.property) } />
                {
                  if (r.alreadyInDatabase) {
                    {println("r.alreadyInDatabase " + r ) }
                    <input value={ r.value.toString } name={ "ORIG-RES-" + urlEncode(r.property) } type="hidden"/>
                  }
                }
              </div>
          } else
          		<a href={ Form2HTML.createHyperlinkString(hrefPrefix, r.value.toString) }>{
                r.value.toString
              }</a>}
        case r: BlankNodeEntry =>
          {if (editable) {
             <input class="form-control" value={ r.value.toString } name={ "BLA-" + urlEncode(r.property) } />
             <input value={ r.value.toString } name={ "ORIG-BLA-" + urlEncode(r.property) } type="hidden"/>
          }else
          	<a href={ Form2HTML.createHyperlinkString(hrefPrefix, r.value.toString, true) }>{
              r.getId
            }</a>
          }
      }

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
