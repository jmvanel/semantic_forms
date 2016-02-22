package deductions.runtime.html

import java.net.URLEncoder
import scala.Range
import scala.xml.Elem
import scala.xml.NodeSeq
import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.Text
import scala.xml.Unparsed
import Form2HTML.urlEncode
import deductions.runtime.abstract_syntax.DBPediaLookup
import deductions.runtime.abstract_syntax.FormModule
import deductions.runtime.utils.I18NMessages
import deductions.runtime.utils.Timer
import deductions.runtime.services.Configuration

/**
 * different modes: display or edit;
 *  takes in account datatype
 */
private [html] trait Form2HTML[NODE, URI <: NODE]
    extends Form2HTMLBase[NODE, URI]
    with Form2HTMLDisplay[NODE, URI]
    with Form2HTMLEdit[NODE, URI]
    with Timer
//    with CSS
    with JavaScript
    with Configuration
    {
  self: HTML5Types =>


  /**
   * render the given Form Syntax as HTML;
   *  @param hrefPrefix URL prefix pre-pended to created ID's for Hyperlink
   *  @param actionURI, actionURI2 HTML actions for the 2 submit buttons
   *  @param graphURI URI for named graph to save user inputs
   */
  def generateHTML(form: FormModule[NODE, URI]#FormSyntax,
                   hrefPrefix: String = "",
                   editable: Boolean = false,
                   actionURI: String = "/save", graphURI: String = "",
                   actionURI2: String = "/save", lang: String = "en"): NodeSeq = {

    val htmlFormFields = time("generateHTMLJustFields",
      generateHTMLJustFields(form, hrefPrefix, editable, graphURI))

    def wrapFieldsWithForm(htmlFormFields: NodeSeq): NodeSeq =
      <div class="container-fluid">  
      	<div class="row">
					<form action={ actionURI } method="POST">
        		<p class="text-right">
          		<input value={ mess("SAVE") } type="submit" class="btn btn-primary btn-lg"/>
        		</p>
            { htmlFormFields }
        		<p class="text-right">
          		<input value={ mess("SAVE") } type="submit" formaction={ actionURI2 } class="btn btn-primary btn-lg pull-right"/>
        		</p>
      		</form>
				</div>
			</div>
    def mess(m: String): String = message(m, lang)

    if (editable)
      wrapFieldsWithForm(htmlFormFields)
    else
      htmlFormFields
  }
  
  /**
   * generate HTML, but Just Fields;
   *  this lets application developers create their own submit button(s) and <form> tag
   */
  def generateHTMLJustFields(form: fm#FormSyntax,
    hrefPrefix: String = "",
    editable: Boolean = false,
    graphURI: String = ""): NodeSeq = {

    implicit val formImpl: fm#FormSyntax = form

    val hidden = if (editable) {
      <input type="hidden" name="url" value={ urlEncode(form.subject) }/>
      <input type="hidden" name="graphURI" value={ urlEncode(graphURI) }/>
    } else Seq()
    hidden ++
      <div class={ css.cssClasses.formRootCSSClass }>
        {
          css.localCSS ++
            Text("\n") ++
            (if( inlineJavascriptInForm )
               localJS
//              css.cssRules
            else Text("")) ++
            Text("\n")
        }
        <input type="hidden" name="uri" value={ urlEncode(form.subject) }/>
        {
          val fields = form.fields
          if (!fields.isEmpty) {
            val lastEntry = fields.last
            ( for ((preceding, field) <- (lastEntry +: fields) zip fields
                // do not display NullResourceEntry
                if( field.property.toString != "" )
            ) yield {
              <div class={ css.cssClasses.formLabelAndInputCSSClass }>
                { makeFieldLabel(preceding, field) }
                { makeFieldDataOrInput(field, hrefPrefix, editable) }
              </div>
            }
            )
//            ++
//            ( for (field <- fields ) yield {
//            	makeFieldDatalist(field)     
//            } )
          }
        }
      </div>
  }


  private def createHTMLField(field: fm#Entry, editable: Boolean,
    hrefPrefix: String = "")(implicit form: FormModule[NODE, URI]#FormSyntax): xml.NodeSeq = {
    
    // hack instead of true form separator in the form spec in RDF:
    if (field.label.contains("----"))
      return <hr style="background:#F87431; border:0; height:4px"/> // Text("----")

    val xmlField = field match {
      case l: fm#LiteralEntry =>
          if (editable)
            createHTMLiteralEditableField(l)
          else
            createHTMLiteralReadonlyField(l)
            
      case r: fm#ResourceEntry =>
        /* link to a known resource of the right type,
           * or (TODO) create a sub-form for a blank node of an ancillary type (like a street address),
           * or just create a new resource with its type, given by range, or derived
           * (like in N3Form in EulerGUI ) */
          if (editable)
            createHTMLResourceEditableField(r)
          else
            createHTMLResourceReadonlyField(r, hrefPrefix)

      case r: fm#BlankNodeEntry =>
          if (editable)
            createHTMLBlankNodeEditableField(r)
          else
            createHTMLBlankNodeReadonlyField(r, hrefPrefix)
      case _ => <p>Should not happen! createHTMLField({ field })</p>
    }

    Seq(createAddRemoveWidgets(field, editable)) ++ xmlField
  }

  /** make Field Data (display) Or Input (edit) */
  private def makeFieldDataOrInput(field: fm#Entry, hrefPrefix: String,
    editable: Boolean)(implicit form: FormModule[NODE, URI]#FormSyntax) = {
    if (shouldAddAddRemoveWidgets(field, editable))
      createHTMLField(field, editable, hrefPrefix)
    else
      // that's for corporate_risk:
      <div class={ css.cssClasses.formInputCSSClass }>
        { createHTMLField(field, editable, hrefPrefix) }
      </div>
  }
}

object Form2HTML {
  def urlEncode(node: Any) = { URLEncoder.encode(node.toString, "utf-8") }

  def createHyperlinkString(hrefPrefix: String, uri: String, blanknode: Boolean = false): String = {
    if (hrefPrefix == "")
      uri
    else {
      val suffix = if (blanknode) "&blanknode=true" else ""
      hrefPrefix + urlEncode(uri) + suffix
    }
  }
}
