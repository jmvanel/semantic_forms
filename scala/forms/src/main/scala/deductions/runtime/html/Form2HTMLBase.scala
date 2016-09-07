package deductions.runtime.html

import scala.util.Try

import Form2HTML.urlEncode
import deductions.runtime.abstract_syntax.FormModule
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.Configuration
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.utils.I18NMessages
import deductions.runtime.utils.RDFPrefixes

/** generate HTML from abstract Form : common parts for Display & edition */
trait Form2HTMLBase[NODE, URI <: NODE]
    extends Configuration
    with BasicWidgets
    // CSS
    {

  type fm = FormModule[NODE, URI]
  type Entry = fm#Entry
  
  lazy val prefixes = new ImplementationSettings.RDFModule
  with RDFPrefixes[ImplementationSettings.Rdf] with DefaultConfiguration {}
  import prefixes._
  
  def makeFieldLabel(preceding: fm#Entry, field: fm#Entry)
  (implicit form: FormModule[NODE, URI]#FormSyntax) = {
    // display field label only if different from preceding
    if (preceding.label != field.label)
      <label class={ css.cssClasses.formLabelCSSClass } title={
        labelTooltip(field)
      } for={ makeHTMLNameResource(field) }>{
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

  def labelTooltip(field: fm#Entry) = {
    val details = if( displayTechnicalSemWebDetails )
        s"""
          property: ${field.property} -
          type: ${field.type_}"""
          else ""
      s"""${field.comment} - $details"""
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
  def makeHTMLName(ent: fm#Entry)(implicit form: FormModule[NODE, URI]#FormSyntax): String = {
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

  def makeHTMLNameResource(re: fm#Entry)(implicit form: FormModule[NODE, URI]#FormSyntax) = makeHTMLName(re)
  def makeHTMLIdResourceSelect(re: fm#Entry)(implicit form: FormModule[NODE, URI]#FormSyntax): String =
    toPlainString(re.property)
  def makeHTMLNameBN(re: fm#Entry)(implicit form: FormModule[NODE, URI]#FormSyntax) = makeHTMLName(re)
  def makeHTMNameLiteral(lit: fm#LiteralEntry)(implicit form: FormModule[NODE, URI]#FormSyntax) = 
    makeHTMLName(lit)
   
  def makeHTML_Id(entry: fm#Entry)(implicit form: FormModule[NODE, URI]#FormSyntax) =
    "f" + form.fields.indexOf(entry)

}
