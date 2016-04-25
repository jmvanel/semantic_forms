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
    extends Form2HTMLBase[NODE, URI] {
  self: HTML5Types 
        with Configuration 
        =>

  // val radioForIntervals = false // choice moved to FormSyntaxFactory
  val inputSize = 90


  def shouldAddAddRemoveWidgets(field: fm#Entry, editable: Boolean)
    (implicit form: FormModule[NODE, URI]#FormSyntax): Boolean = {
    // println( "showPlusButtons " + showPlusButtons)
    editable && field.defaults.multivalue && field.openChoice && showPlusButtons &&
    isFirstFieldForProperty(field)
  }
  
  def createAddRemoveWidgets(field: fm#Entry, editable: Boolean)
  (implicit form: FormModule[NODE, URI]#FormSyntax): Elem = {
    if (shouldAddAddRemoveWidgets(field, editable)) {
      // button with an action to duplicate the original HTML widget with an empty content
      val widgetName = field match {
        case r: fm#ResourceEntry if lookup(r) => makeHTML_Id(r)
        case r: fm#ResourceEntry => makeHTMLIdResourceSelect(r)
        case _ => makeHTML_Id(field)
      }
      <div class={ css.cssClasses.formAddDivCSSClass }>
      <button class="btn btn-primary" readonly="yes" size="1" title={
        "Add another value for " + field.label } onClick={
        s""" cloneWidget( "$widgetName" ); return false;"""
      }><i class="glyphicon glyphicon-plus"></i></button>
			</div>
    } else <span></span>
  }

  /** tell if given Entry is configured for lookup (completion) */
  def lookup(r: fm#Entry) = r.widgetType == DBPediaLookup
  
  /** create HTM Literal Editable Field, taking in account owl:DatatypeProperty's range */
  def createHTMLResourceEditableField(r: fm#ResourceEntry
      )(implicit form: FormModule[NODE, URI]#FormSyntax): NodeSeq = {
//  if( r.property . toString() . contains("knows")) println("knows")
    val placeholder = resourcePlaceholder(r)
    Seq(
      Text("\n"),
    		// format: OFF
        if (r.openChoice)
          <div class={ css.cssClasses.formDivInputCSSClass }>
          <input class={ css.cssClasses.formInputCSSClass }
      			value={ r.value.toString }
            name={ makeHTMLNameResource(r) }
            id={ makeHTML_Id(r) }
            list={ makeHTMLIdForDatalist(r) }
            data-type={ r.type_.toString() }
            placeholder={ placeholder }
            title={ placeholder }
            onkeyup={if (lookup(r)) "onkeyupComplete(this);" else null}
            size={inputSize.toString()}
						dropzone="copy">
          </input>
          { if (lookup(r))
            <script type="text/javascript" >
              addDBPediaLookup('#{ makeHTML_Id(r) }'); 
            </script> }
					</div>
				else new Text("") // format: ON
      ,
      if (lookup(r))
        formatPossibleValues(r, inDatalist = true)
      else renderPossibleValues(r)
      , Text("\n")
    ). flatten
  }

  /** Placeholder for a resource URI or literal */
  private def resourcePlaceholder(r: fm#Entry) =
    if (lookup(r))
      s"Enter a word; completion with Wikipedia lookup"
      else {
        val typ0 = r.type_.toString()
        val typ = if (
            typ0 == "" ||
            typ0.endsWith( "#Thing") )
          ""
        else
          " of type <" + typ0 + ">"
        s"Enter or paste a resource URI $typ; typing here creates a new resource; better look first in pulldown menu for an already existing resource."
      }

  /**
   * TODO analyse differences with createHTMLResourceEditableField;
   *  maybe there is no necessary difference
   */
  def createHTMLBlankNodeEditableField(r: fm#BlankNodeEntry)(implicit form: FormModule[NODE, URI]#FormSyntax): NodeSeq = {
    val placeholder = resourcePlaceholder(r)
    Seq(
      if (r.openChoice) {
        <div class={ css.cssClasses.formDivInputCSSClass }>
        <input class={ css.cssClasses.formInputCSSClass } value={
          r.value.toString
        } name={ makeHTMLNameBN(r) } id={
          makeHTML_Id(r)
        } data-type={
          r.type_.toString()
        } size={ inputSize.toString() }
        placeholder={ placeholder }
        title={ placeholder }
        >
        </input>
				</div>
      }else new Text("\n")
      ,
      renderPossibleValues(r)) . flatten
  }

  def renderPossibleValues(r: fm#Entry)(implicit form: FormModule[NODE, URI]#FormSyntax): NodeSeq = {
    if (hasPossibleValues(r) &&
      isFirstFieldForProperty(r)) {
      <div class={ css.cssClasses.formSelectDivCSSClass }>
      <select class={ css.cssClasses.formSelectCSSClass } value={ r.value.toString } name={
        makeHTMLNameResource(r) } id={
        makeHTMLIdResourceSelect(r)
      } list={
        makeDatalistIdForEntryProperty(r)
      }>
        { formatPossibleValues(r) }
      </select>
			</div>
    } else new Text("\n")
  }
  
  def makeDatalistIdForEntryProperty(r: fm#Entry) = urlEncode(r.property.toString()) + "__property"



  //// stuff for Literal (string) data ////

  /** create HTM Literal Editable Field, taking in account owl:DatatypeProperty's range */
  def createHTMLiteralEditableField(lit: fm#LiteralEntry)(implicit form: FormModule[NODE, URI]#FormSyntax): NodeSeq = {
    val placeholder = {
        val typ0 = lit.type_.toString()
        val typ = if (
            typ0 == ""
            || typ0.endsWith( "#string")
            || typ0.endsWith( "#Literal")
        )
          ""
        else
          " of type <" + typ0 + ">"
        s"Enter or paste a string $typ"
    }

    val htmlId = makeHTML_Id(lit) // "f" + form.fields.indexOf(lit)
    val elem = lit.type_.toString() match {

      // TODO in FormSyntaxFactory match graph pattern for interval datatype ; see issue #17
      case t if t == ("http://www.bizinnov.com/ontologies/quest.owl.ttl#interval-1-5") =>
        if (radioForIntervals)
          (for (n <- Range(0, 6)) yield (
            <input type="radio" name={ makeHTMNameLiteral(lit) } id={
              makeHTMNameLiteral(lit) } checked={
              if (n.toString.equals(lit.value)) "checked" else null
            } value={ n.toString }>
            </input>
            <label for={ makeHTMNameLiteral(lit) }>{ n }</label>
          )).flatten
        else {
          // TODO maybe call re()nderPossibleValues
          <select name={ makeHTMNameLiteral(lit) }>
            { formatPossibleValues(lit) }
          </select>
        }

      case _ =>
        <div class={ css.cssClasses.formDivInputCSSClass }>
        <input class={ css.cssClasses.formInputCSSClass } value={
          toPlainString(lit.value)
        } name={ makeHTMNameLiteral(lit) } type={
          xsd2html5TnputType(lit.type_.toString())
        }
        placeholder={ placeholder }
        title={ placeholder }
        size={
          inputSize.toString()
        } dropzone="copy" id={ htmlId }>
        </input></div>
        <div class={ css.cssClasses.formDivEditInputCSSClass }>
				{ if( showEditButtons )
        <input type="button" value="EDIT"
				onClick={
          s"""launchEditorWindow( document.getElementById( "$htmlId" ));"""
        } title="Click to edit multiline text in popup window as Markdown text">
        </input>
				}
				</div>
    }
    Text("\n") ++ elem
  }

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
    		makeHTMLOptionsSequence(field)
    if (inDatalist)
      <datalist id={ makeHTMLIdForDatalist(field) }>
        { options }
      </datalist>
    else options
  }

  /** make sequence of HTML <option> */
  private def makeHTMLOptionsSequence(field: Entry)
  (implicit form: fm#FormSyntax) = {
	  def makeHTMLOption(value: (NODE, NODE), field: fm#Entry): Elem = {
		  <option value={ toPlainString(value._1) } selected={
			  if (toPlainString(field.value) ==
					  toPlainString(value._1))
			    "selected"
			  else
			    null
		  } title={ toPlainString(value._1) } label={ toPlainString(value._2) }>{
		    toPlainString(value._2) }
		  </option>
	  }
	  def getPossibleValues( f: Entry) = form.possibleValuesMap.getOrElse( f.property, Seq() )
	  for (value <- getPossibleValues(field) )
	    yield makeHTMLOption(value, field)
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
