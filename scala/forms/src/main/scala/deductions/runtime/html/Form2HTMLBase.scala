package deductions.runtime.html

import Form2HTML.urlEncode
import deductions.runtime.abstract_syntax.FormModule
import deductions.runtime.utils.I18NMessages
import deductions.runtime.services.Configuration
import scala.util.Try

/** generate HTML from abstract Form : common parts for Display & edition */
trait Form2HTMLBase[NODE, URI <: NODE]
    extends Configuration
    // CSS
    {

  type fm = FormModule[NODE, URI]
  type Entry = fm#Entry
  
  def makeFieldLabel(preceding: fm#Entry, field: fm#Entry)
  (implicit form: FormModule[NODE, URI]#FormSyntax) = {
    // display field label only if different from preceding
    if (preceding.label != field.label)
      <label class={ css.cssClasses.formLabelCSSClass } title={
        field.comment + " - " + field.property
      } for={ makeHTMLIdResource(field) }>{
        val label = field.label
        // hack before implementing real separators
        if (label.contains("----"))
          label.substring(1).replaceAll("-(-)+", "")
        else label
      }</label>
    else
      <label class={ css.cssClasses.formLabelCSSClass } title={
        field.comment + " - " + field.property
      }> -- </label>
  }

  //  def toPlainString[NODE](n: NODE): String = n.toString()
  def toPlainString(n: NODE): String = n.toString()
  
  def message(m: String,lang: String): String = I18NMessages.get(m, lang)

  def isFirstFieldForProperty( field: fm#Entry )
    (implicit form: FormModule[NODE, URI]#FormSyntax): Boolean = {
    val ff = form.fields
    val previous = Try(ff(ff.indexOf(field) - 1)).toOption
    previous match {
      case Some(fi) => fi.property != field.property
      case None => true
    }
  }


    /** leveraging on HTTP parameter being the original triple from TDB,
   * in N-Triple syntax, we generate here the HTTP parameter from the original triple;
   * see HttpParamsManager#httpParam2Triple for the reverse operation */
  def makeHTMLId(ent: fm#Entry)(implicit form: FormModule[NODE, URI]#FormSyntax): String = {
    val rawResult = {
      def makeTTLURI(s: NODE) = s"<$s>"
      def makeTTLBN(s: NODE) = s"_:$s"
      def makeTTLAnyTerm(value: NODE, ent: fm#Entry) = {
        ent match {
          case lit: fm#LiteralEntry => value
          case bn: fm#BlankNodeEntry => makeTTLBN(value)
          case _ => makeTTLURI(value)
        }
      }
      makeTTLURI(form.subject) + " " +
        makeTTLURI(ent.property) + " " +
        makeTTLAnyTerm(ent.value, ent) + " .\n"
    }
    urlEncode(rawResult)
  }

  def makeHTMLIdResource(re: fm#Entry)(implicit form: FormModule[NODE, URI]#FormSyntax) = makeHTMLId(re)
  def makeHTMLIdResourceSelect(re: fm#Entry)(implicit form: FormModule[NODE, URI]#FormSyntax): String =
    toPlainString(re.property)
  def makeHTMLIdBN(re: fm#Entry)(implicit form: FormModule[NODE, URI]#FormSyntax) = makeHTMLId(re)
  def makeHTMLIdForLiteral(lit: fm#LiteralEntry)(implicit form: FormModule[NODE, URI]#FormSyntax) =
    makeHTMLId(lit)
    
}
