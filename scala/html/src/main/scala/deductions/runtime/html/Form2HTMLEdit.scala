package deductions.runtime.html

import deductions.runtime.core.{DBPediaLookup, FormModule}
import deductions.runtime.utils.{Configuration, I18NMessages, RDFPrefixesInterface}
import org.joda.time.DateTime

import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.{Elem, NodeSeq, Text}
import deductions.runtime.core.URIWidget


/** generate HTML from abstract Form for Edition */
private[html] trait Form2HTMLEdit[NODE, URI <: NODE]
    extends Form2HTMLBase[NODE, URI]
  with RDFPrefixesInterface{

  self: HTML5Types =>

  val config: Configuration
  import config._

  val inputSize = 90
  val xsdPrefix = "http://www.w3.org/2001/XMLSchema#"

  def shouldAddAddRemoveWidgets(field: formMod#Entry, editable: Boolean)
    (implicit form: FormModule[NODE, URI]#FormSyntax): Boolean = {
    // println( "showPlusButtons " + showPlusButtons)
    editable && field.defaults.multivalue && field.openChoice && showPlusButtons &&
    isFirstFieldForProperty(field)
  }

  def createAddRemoveWidgets(field: formMod#Entry, editable: Boolean)
  (implicit form: FormModule[NODE, URI]#FormSyntax): Elem = {
    if (shouldAddAddRemoveWidgets(field, editable)) {

      // button with an action to duplicate the original HTML widget with an empty content

      val lookupCSSClass =
        if (field.widgetType == DBPediaLookup)
          "hasLookup"
        else if (field.widgetType == URIWidget)
          "sfLookup"
        else ""
      <div class={ css.cssClasses.formAddDivCSSClass } > <!-- TODO : hidden="true" -->
      <button type="button" class="btn btn-primary add-widget" readonly="yes" size="1" title={
        "Add another value for " + field.label } input-class={s"${css.cssClasses.formInputCSSClass} $lookupCSSClass"} input-name={field.htmlName} input-title={ resourceOrBN_Placeholder(field) }  >
        <i class="glyphicon glyphicon-plus"></i>
      </button>
			</div>
    } else <span></span>
  }

  /** tell if given Entry is configured for lookup (completion) */
  def lookupActivatedFor(r: formMod#Entry) = r.widgetType == DBPediaLookup

  /** create HTM Literal Editable Field, taking in account owl:DatatypeProperty's range */
  def createHTMLResourceEditableField(resourceEntry: formMod#ResourceEntry, lang: String = "en"
      )(implicit form: FormModule[NODE, URI]#FormSyntax): NodeSeq = {
    import resourceEntry._
    val placeholder = resourcePlaceholder(resourceEntry, lang)
    val hasLookup = if (lookupActivatedFor(resourceEntry) ) "hasLookup" else "sfLookup"
    Seq(
      Text("\n"),
    		// format: OFF
        if (openChoice)
          <div class={ css.cssClasses.formDivInputCSSClass }>
            {
      addTripleAttributesToXMLElement(
          <input class={ css.cssClasses.formInputCSSClass +
            " " + hasLookup }
            value={ value.toString }
            name={
              // makeHTMLName(resourceEntry)
              resourceEntry.htmlName
            }
            id={ makeHTML_Id(resourceEntry) }
            list={ makeHTMLIdForDatalist(resourceEntry) }
            placeholder={ placeholder }
            title={ type_.toString() }
            size={inputSize.toString()}
			dropzone="copy"
            autocomplete={if (lookupActivatedFor(resourceEntry)) "off" else null}
            >
          </input> , resourceEntry) }
            {makeUserInfoOnTriples(resourceEntry.metadata,resourceEntry.timeMetadata)}
		  </div>
		else new Text("") // format: ON
      ,
      if (lookupActivatedFor(resourceEntry))
        formatPossibleValues(resourceEntry, inDatalist = true)

      /* TODO
       *  rather test in form generation, to avoid useless processing
       *  instead of relying on config:
       *  - detect mobile
       *  - detect slow Internet connection
       */
      else if( config.downloadPossibleValues)
        renderPossibleValues(resourceEntry)
      else NodeSeq.Empty
      , Text("\n")
    ). flatten
  }

  /** Placeholder for a resource URI */
  private def resourcePlaceholder(resourceEntry: formMod#ResourceEntry, lang: String = "en") = {
    resourceOrBN_Placeholder(resourceEntry, lang )  +
        " - " + resourceEntry.valueLabel
  }
  private def resourceOrBN_Placeholder(r: formMod#Entry, lang: String = "en") = {
    if (lookupActivatedFor(r))
      I18NMessages.get("Completion", lang)
//      s"Enter a word; completion with Wikipedia lookup"
      else {
        val typ0 = r.type_.toString()
        val typ = if (
            typ0 == "" ||
            typ0.endsWith( "#Thing") )
          ""
        else
          abbreviateTurtle(typ0)
//          prefixes.abbreviateTurtle(typ0)
        I18NMessages.format("Enter_resource_URI", lang, typ)
//        s"Enter or paste a resource URI  of type $typ; typing here creates a new resource; better look first in pulldown menu for an already existing resource."
      }
}

  /**
   * TODO analyse differences with createHTMLResourceEditableField;
   *  maybe there is no necessary difference
   */
  def createHTMLBlankNodeEditableField(r: formMod#BlankNodeEntry)(implicit form: FormModule[NODE, URI]#FormSyntax): NodeSeq = {
    val placeholder = resourceOrBN_Placeholder(r)
    Seq(
      if (r.openChoice) {
        <div class={ css.cssClasses.formDivInputCSSClass }>
        {addTripleAttributesToXMLElement(
        <input class={ css.cssClasses.formInputCSSClass } value={
          r.value.toString
        } name={
          // makeHTMLNameBN(r)
          r.htmlName } id={
          makeHTML_Id(r)
        } size={ inputSize.toString() }
        placeholder={ placeholder }
        title={ placeholder }
        >
        </input> , r) }
        {makeUserInfoOnTriples(r.metadata,r.timeMetadata)}
				</div>
      }else new Text("\n")
      ,
      renderPossibleValues(r)) . flatten
  }

  /** display list of Possible Values from TDB as a <select> pulldown menu */
  private def renderPossibleValues(r: formMod#Entry)(implicit form: FormModule[NODE, URI]#FormSyntax): NodeSeq = {
    if (hasPossibleValues(r) &&
      isFirstFieldForProperty(r)) {
      <div class={ css.cssClasses.formSelectDivCSSClass }>
        <select class={ css.cssClasses.formSelectCSSClass } value={ r.value.toString } name={
//          makeHTMLName(r)
          r.htmlName
        } id={
          makeHTMLIdResourceSelect(r)
        } list={
          makeDatalistIdForEntryProperty(r)
        }>
          { formatPossibleValues(r) }
        </select>
      </div>
    } else new Text("\n")
  }

  def makeDatalistIdForEntryProperty(r: formMod#Entry) = urlEncode(r.property.toString()) + "__property"



  //// stuff for Literal (string) data ////

  /** create HTM Literal Editable Field, taking in account owl:DatatypeProperty's range */
  def createHTMLiteralEditableField(lit: formMod#LiteralEntry)(implicit form: FormModule[NODE, URI]#FormSyntax): NodeSeq = {
    import lit._
    val placeholder = {
        val typ0 = type_.toString()
        val typ = if (
            typ0 == ""
            || typ0.endsWith( "#string")
            || typ0.endsWith( "#Literal")
        )
          ""
        else
          s" of type <$typ0>"
        s"Enter or paste a string $typ"
    }

    val htmlId = makeHTML_Id(lit) // "f" + form.fields.indexOf(lit)
    val elem = type_.toString() match {

      // TODO in FormSyntaxFactory match graph pattern for interval datatype ; see issue #17
      case typ if typ == ("http://www.bizinnov.com/ontologies/quest.owl.ttl#interval-1-5") =>
        if (radioForIntervals)
          (for (n <- Range(0, 6)) yield (
            <input type="radio" name={
              // makeHTMLName(lit)
              lit.htmlName } id={
//              makeHTMLName(lit)
              lit.htmlName } checked={
              if (n.toString.equals(value)) "checked" else null
            } value={ n.toString }>
            </input>
            <label for={
              // makeHTMLName(lit)
              lit.htmlName }>{ n }</label>
          )).flatten
        else {
          // TODO maybe call renderPossibleValues
          <select name={
//            makeHTMLName(lit)
            lit.htmlName }>
            { formatPossibleValues(lit) }
          </select>
        }

      case typ if typ == xsdPrefix + "boolean" =>
        <div class="wrapper">
          <label for="yes_radio" id="yes-lbl">Oui</label>
          <input type="radio" name={
//            makeHTMLName(lit)
            lit.htmlName } id="yes_radio"
          checked={toPlainString(value) match {case "true" => "true" ; case _ => null } }
          value="true" ></input>

          <label for="maybe_radio" id="maybe-lbl">?</label>
          <input type="radio" name={
            // makeHTMLName(lit)
            lit.htmlName } id="maybe_radio"
          checked={toPlainString(value) match {case "" => "checked" ; case _ => null } }
          value="" ></input>

          <label for="no_radio" id="no-lbl">Non</label>
          <input type="radio" name={
            // makeHTMLName(lit)
            lit.htmlName } id="no_radio"
          checked={toPlainString(value) match {case "false" => "false" ; case _ => null } }
          value="false" ></input>
          <div class="toggle"></div>
        </div>

      case _ =>
        <div class={ css.cssClasses.formDivInputCSSClass }>
        {addTripleAttributesToXMLElement(
          <input class={ css.cssClasses.formInputCSSClass } value={
            toPlainString(value)
          } name={
            // makeHTMLName(lit)
            lit.htmlName } type={
            xsd2html5TnputType(type_.toString())
          }
          step = {xsd2html5Step(type_.toString())}
          placeholder={ placeholder } title={ placeholder } size={
            inputSize.toString()
          } dropzone="copy" id={ htmlId }
          >
          </input> ,
          lit ) }
          { makeUserInfoOnTriples(lit.metadata, lit.timeMetadata) }
        </div>
        <div class={ css.cssClasses.formDivEditInputCSSClass }>
          {
            if (showEditButtons)
              <input class="btn btn-primary" type="button" value="EDIT" onClick={
                s"""launchEditorWindow( document.getElementById( "$htmlId" ));"""
              } title="Click to edit multiline text in popup window as Markdown text">
              </input>
          }
        </div>
    }
    Text("\n") ++ elem
  }

  private def makeHTMLIdForDatalist(re: formMod#Entry): String = {
    "possibleValues-" + (
      re match {
        case re: formMod#ResourceEntry => (re.property + "--" + re.value).hashCode().toString()
        case lit: formMod#LiteralEntry => (lit.property + "--" + lit.value).hashCode().toString()
        case bn: formMod#BlankNodeEntry => (bn.property + "--" + bn.value).hashCode().toString()
      })
  }

  /** @return a list of option tags or a datalist tag (with the option tags inside) */
  def formatPossibleValues(field: FormEntry, inDatalist: Boolean = false)
  (implicit form: formMod#FormSyntax)
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
  private def makeHTMLOptionsSequence(field: FormEntry)
  (implicit form: formMod#FormSyntax): Seq[Elem] = {
	  def makeHTMLOption(value: (NODE, NODE), field: formMod#Entry): Elem = {
		  <option value={ toPlainString(value._1) } selected={
			  if (toPlainString(field.value) ==
					  toPlainString(value._1))
			    "selected"
			  else null
		  } title={ toPlainString(value._1) } label={ toPlainString(value._2) }>{
		    toPlainString(value._2) }
		  </option>
	  }
	  def getPossibleValues( f: FormEntry) =
	    form.possibleValuesMap.getOrElse( f.property, Seq() )

	  for (value <- getPossibleValues(field) )
	    yield makeHTMLOption(value, field)
  }


  def hasPossibleValues( f: FormEntry)(implicit form: formMod#FormSyntax): Boolean = {
//    println( s">>>> hasPossibleValues ${f.label} : ${form.possibleValuesMap.getOrElse(f.property, "NOTHING")}" )
    ! form.possibleValuesMap.getOrElse( f.property, List() ) .isEmpty
  }

  private val datalistsAlreadyDone = scala.collection.mutable.Set[NODE]()
	def	makeFieldDatalist(field: FormEntry)
		  (implicit form: formMod#FormSyntax)
		  : Elem
		  = {
	  if( ! datalistsAlreadyDone.contains(field.property) ) {
		  datalistsAlreadyDone.add(field.property)
		  <datalist id={ makeDatalistIdForEntryProperty(field) }>
		  { makeHTMLOptionsSequence(field) }
		  </datalist>
	  } else <div></div>
  }

  private def makeUserInfoOnTriples(userMetadata: String,timeMetadata: Long): Elem ={
    val time :String = new DateTime(timeMetadata).toDateTime.toString("dd/MM/yyyy HH:mm")
    if (timeMetadata != -1){
      <p>
        modifié par:{userMetadata} le {time}
      </p>
    }
    else <p></p>
  }

}
