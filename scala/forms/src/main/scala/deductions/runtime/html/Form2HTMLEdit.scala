package deductions.runtime.html

import scala.Range
import scala.xml.Elem
import scala.xml.NodeSeq
import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.Text
import Form2HTML.urlEncode
import deductions.runtime.abstract_syntax.DBPediaLookup
import deductions.runtime.abstract_syntax.FormModule
import deductions.runtime.services.Configuration


/** generate HTML from abstract Form for Edition */
trait Form2HTMLEdit[NODE, URI <: NODE]
    extends Form2HTMLBase[NODE, URI]
        {
  self: HTML5Types 
        with Configuration 
        =>
//  import Configuration._

  val radioForIntervals = false // TODO the choice should be moved to FormSyntaxFactory
  val inputSize = 90


  def shouldAddAddRemoveWidgets(field: fm#Entry, editable: Boolean): Boolean = {
    println( "showPlusButtons " + showPlusButtons)
    editable && field.defaults.multivalue && field.openChoice && showPlusButtons
  }
  def createAddRemoveWidgets(field: fm#Entry, editable: Boolean)(implicit form: FormModule[NODE, URI]#FormSyntax): Elem = {
    if (shouldAddAddRemoveWidgets(field, editable)) {
      // button with an action to duplicate the original HTML widget with an empty content
      val widgetName = makeHTMLId(field)
      <input value="+" class="button-add btn-primary" readonly="yes" size="1" title={ "Add another value for " + field.label } onClick={
        s""" cloneWidget( "$widgetName" ); """
      }></input>
    } else <span></span>
  }


//  lazy private val propertySelectAlreadyDone = scala.collection.mutable.Set[NODE]()
  lazy private val propertySelectAlreadyDone = scala.collection.mutable.Set[Any]()
  /** create HTM Literal Editable Field, taking in account owl:DatatypeProperty's range */
  def createHTMLResourceEditableField(r: fm#ResourceEntry
      )(implicit form: FormModule[NODE, URI]#FormSyntax): NodeSeq = {
    val lookup = r.widgetType == DBPediaLookup
//    if( r.property . toString() . contains("knows")) println("knows")
    Seq(
      Text("\n"),
    		// format: OFF
        if (r.openChoice)
          <input class={ cssClasses.formInputCSSClass } value={ r.value.toString }
            name={ makeHTMLIdResource(r) }
            list={ makeHTMLIdForDatalist(r) }
            data-type={ r.type_.toString() }
            placeholder={ if (lookup)
              s"Enter a word; completion with Wikipedia lookup"
              else
              s"Enter or paste a resource URI of type ${r.type_.toString()}" }
            onkeyup={if (lookup) "onkeyupComplete(this);" else null}
            size={inputSize.toString()}
						dropzone="copy">
          </input> else new Text("") // format: ON
          ,
      if (lookup)
        formatPossibleValues(r, inDatalist = true)
      else new Text(""),

//      if (!r.possibleValues.isEmpty && !lookup)
      if ( hasPossibleValues(r) && !lookup)
    	  if( ! propertySelectAlreadyDone.contains(r.property) ) {
    		  propertySelectAlreadyDone.add(r.property)
    		  <select value={ r.value.toString } name={ makeHTMLIdResource(r) } list={
    			  makeDatalistIdForEntryProperty(r) }>
    		  { formatPossibleValues(r) }
    		  </select>
    	  } else new Text("\n")
    	  else new Text("\n")
    ).flatMap { identity }
  }

  def createHTMLBlankNodeEditableField(r: fm#BlankNodeEntry)(implicit form: FormModule[NODE, URI]#FormSyntax): NodeSeq = {
    if (r.openChoice) {
      <input class={ cssClasses.formInputCSSClass } value={
        r.value.toString
      } name={ makeHTMLIdBN(r) } data-type={
        r.type_.toString()
      } size={ inputSize.toString() }>
      </input>
    }
    if (!r.possibleValues.isEmpty)
      <select value={ r.valueLabel } name={ makeHTMLIdBN(r) }>
        { formatPossibleValues(r) }
      </select>
    else Seq()
  }
    
  def makeDatalistIdForEntryProperty(r: fm#Entry) = urlEncode(r.property.toString()) + "__property"
    
  /** create HTM Literal Editable Field, taking in account owl:DatatypeProperty's range */
  def createHTMLiteralEditableField(lit: fm#LiteralEntry)(implicit form: FormModule[NODE, URI]#FormSyntax): NodeSeq = {
    val placeholder = s"Enter or paste a string of type ${lit.type_.toString()}"

    val htmlId = "f" + form.fields.indexOf(lit)
    val elem = lit.type_.toString() match {

      // TODO in FormSyntaxFactory match graph pattern for interval datatype ; see issue #17
      case t if t == ("http://www.bizinnov.com/ontologies/quest.owl.ttl#interval-1-5") =>
        if (radioForIntervals)
          (for (n <- Range(0, 6)) yield (
            <input type="radio" name={ makeHTMLIdForLiteral(lit) } id={ makeHTMLIdForLiteral(lit) } checked={
              if (n.toString.equals(lit.value)) "checked" else null
            } value={ n.toString }>
            </input>
            <label for={ makeHTMLIdForLiteral(lit) }>{ n }</label>
          )).flatten
        else {
          <select name={ makeHTMLIdForLiteral(lit) }>
            { formatPossibleValues(lit) }
          </select>
        }

      case _ =>
        <input class={ cssClasses.formInputCSSClass } value={
          toPlainString(lit.value)
        } name={ makeHTMLIdForLiteral(lit) } type={
          xsd2html5TnputType(lit.type_.toString())
        } placeholder={ placeholder } size={
          inputSize.toString()
        } dropzone="copy" id={ htmlId }>
        </input>
        <input type="button" value="EDIT" onClick={
          s"""launchEditorWindow( document.getElementById( "$htmlId" ));"""
        } title="Click to edit text in popup window as Markdown text">
        </input>
    }
    Text("\n") ++ elem
  }

//  private def makeHTMLIdForDatalist(uri: Rdf#URI): String = {
//      uri.toString()
//    }

  private def makeHTMLIdForDatalist(re: fm#Entry): String = {
    "possibleValues-" + (
      re match {
        case re: fm#ResourceEntry => (re.property + "--" + re.value).hashCode().toString()
        case lit: fm#LiteralEntry => (lit.property + "--" + lit.value).hashCode().toString()
        case bn: fm#BlankNodeEntry => (bn.property + "--" + bn.value).hashCode().toString()
      })
  }

//  /** @return a list of option tags or a datalist tag (with the option tags inside) */
  def formatPossibleValues(field: Entry, inDatalist: Boolean = false)
  (implicit form: fm#FormSyntax)
  : NodeSeq = {
    val options = Seq(<option value=""></option>) ++
//      (for (value <- field.possibleValues) yield makeHTMLOption(value, field))
    		makeHTMLOptionsSequence(field)
    if (inDatalist)
      <datalist id={ makeHTMLIdForDatalist(field) }>
        { options }
      </datalist>
    else options
  }

  def makeHTMLOptionsSequence(field: Entry)
  (implicit form: fm#FormSyntax) = {
	  def makeHTMLOption(values: (NODE, NODE), field: fm#Entry): Elem = {
		  <option value={ toPlainString(values._1) } selected={
			  if (field.value.toString() ==
					  toPlainString(values._1)) "selected" else null
		  } title={ toPlainString(values._1) } label={ toPlainString(values._2) }>{ toPlainString(values._2) }</option>
	  }
	  def getPossibleValues( f: Entry) = form.possibleValuesMap.getOrElse( f.property, Seq() )
	  for (value <- getPossibleValues(field) ) yield makeHTMLOption(value, field)
  }
  
  def hasPossibleValues( f: Entry)(implicit form: fm#FormSyntax) = form.possibleValuesMap.contains( f.property )

  private val datalistsAlreadyDone = scala.collection.mutable.Set[NODE]()
	def	makeFieldDatalist(field: Entry)
		  (implicit form: fm#FormSyntax)
		  : Elem
		  = {
	  if( ! datalistsAlreadyDone.contains(field.property) ) {
		  datalistsAlreadyDone.add(field.property)
		  <datalist id={ makeDatalistIdForEntryProperty(field) }>
		  { makeHTMLOptionsSequence(field) }
		  </datalist>
	  } else <div></div>
  }

}