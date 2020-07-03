package deductions.runtime.html

import deductions.runtime.core.{DBPediaLookup, FormModule}
import deductions.runtime.utils.{Configuration, I18NMessages, RDFPrefixesInterface}
import org.joda.time.DateTime

import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.{Elem, NodeSeq, Text}
import deductions.runtime.core.URIWidget
import deductions.runtime.core.HTTPrequest
import scala.xml.Unparsed

import scalaz._
import Scalaz._
import deductions.runtime.core.ShortString
import scala.xml.NodeBuffer
import deductions.runtime.core.exactlyOne
import deductions.runtime.core.zeroOrOne
import deductions.runtime.core.SPARQLvirtuosoLookup

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
    isFirstFieldForProperty(field) &&
        field.cardinality != exactlyOne &&
        field.cardinality != zeroOrOne
  }

  def createAddRemoveWidgets(field: formMod#Entry, editable: Boolean)
  (implicit form: FormModule[NODE, URI]#FormSyntax): Elem = {
    if (shouldAddAddRemoveWidgets(field, editable)) {

      // button with an action to duplicate the original HTML widget with an empty content

      val lookupCSSClass = lookupCSSclass(field)
      <div class={ cssConfig.formAddDivCSSClass } > <!-- TODO : hidden="true" -->
      <button type="button" class="btn btn-primary add-widget" readonly="yes" size="1" title={
        "Add another value for " + field.label } input-class={s"${cssConfig.formInputCSSClass} $lookupCSSClass"} input-name={field.htmlName} input-title={ resourceOrBN_Placeholder(field) }  >
        <i class="glyphicon glyphicon-plus"></i>
      </button>
			</div>
    } else <span></span>
  }

  /** tell if given Entry is configured for lookup (completion) */
  def lookupActivatedFor(r: formMod#Entry) = r.widgetType == DBPediaLookup

  def lookupCSSclass(entry: formMod#Entry) =
    if (lookupActivatedFor(entry) ) "hasLookup"
      else if (entry.widgetType == SPARQLvirtuosoLookup )
          "virtuosoLookup"
        else if (entry.widgetType == URIWidget)
          "sfLookup"
        else ""

  /** create HTML Resource Editable Field */
  def createHTMLResourceEditableField(resourceEntry: formMod#ResourceEntry, lang: String = "en"
      )(implicit form: FormModule[NODE, URI]#FormSyntax): NodeSeq = {
    import resourceEntry._
    val placeholder = resourcePlaceholder(resourceEntry, lang)
    val hasLookupCSSclass = lookupCSSclass(resourceEntry)
    Seq(
      Text("\n"),
    		// format: OFF
        if (openChoice)
          <div class={ cssConfig.formDivInputCSSClass }>
            {
      addTripleAttributesToXMLElement(
          // <!-- TODO ? dropzone="copy" -->
          <input class={ cssConfig.formInputCSSClass +
            " " + hasLookupCSSclass }
            value={ toPlainString(value) }
            name={ resourceEntry.htmlName }
            id={ makeHTML_Id(resourceEntry) }
            list={ makeHTMLIdForDatalist(resourceEntry) }
            placeholder={ placeholder }
            title={ placeholder }
            size={inputSize.toString()}
            autocomplete={if (lookupActivatedFor(resourceEntry)) "off" else null}
            type={
              /* alas the browsers do NOT consider that mailto:a@b is an email !!!! :(
               * so need to comment this out,
               * otherwise on re-editing the browser blocks the user. */
              //              if( resourceEntry.property.toString() . toLowerCase().endsWith("mbox"))
              //            	  "email"
              //            	else
              if( resourceEntry.property.toString() . toLowerCase().endsWith("phone"))
            		"tel"
              else
                ""}
            >
          </input> , resourceEntry) }
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
      message("Completion", lang)
      else {
        val typ0 = firstNODEOrElseEmptyString(r.type_)
        val typ = if (
            typ0 === "" ||
            typ0.endsWith( "#Thing") )
          ""
        else
          abbreviateTurtle(typ0)
        I18NMessages.format("Enter_resource_URI", lang, typ)
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
        <div class={ cssConfig.formDivInputCSSClass }>
        {addTripleAttributesToXMLElement(
        <input class={ cssConfig.formInputCSSClass } value={
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
				</div>
      }else new Text("\n")
      ,
      renderPossibleValues(r)) . flatten
  }

  /** display list of Possible Values from TDB as a <select> pulldown menu */
  private def renderPossibleValues(r: formMod#Entry)(implicit form: FormModule[NODE, URI]#FormSyntax): NodeSeq = {
    if (hasPossibleValues(r) &&
      isFirstFieldForProperty(r)) {
      <div class={ cssConfig.formSelectDivCSSClass }>
        <select class={ cssConfig.formSelectCSSClass } value={ r.value.toString } name={
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

  /** create HTML Literal Editable Field, taking in account owl:DatatypeProperty's range */
  def createHTMLiteralEditableField(
      lit: formMod#LiteralEntry,
      request: HTTPrequest )(implicit form: FormModule[NODE, URI]#FormSyntax): NodeSeq = {
//    println(s"==== createHTMLiteralEditableField lit $lit")
    import lit._
    val type0 = firstNODEOrElseEmptyString(type_)
    val placeholder = {
        val typ = if (
            type0 === ""
            || type0.endsWith( "#string")
            || type0.endsWith( "#Literal")
        )
          ""
        else
          s" of type <$type0>"
        s"Enter or paste a string <$typ> - '${lit.value}'"
    }

    val htmlId = makeHTML_Id(lit)

    val (input, html5Type) = type0 match {

      case typ if typ === ("http://www.bizinnov.com/ontologies/quest.owl.ttl#interval-1-5") =>
        // TODO in FormSyntaxFactory match graph pattern for interval datatype ; see issue #17
        val html5Type = "radio"
        ( <div>{
        if (radioForIntervals)
          (for (n <- Range(0, 6)) yield (
            <input type={html5Type} name={
              lit.htmlName } id={
              lit.htmlName } checked={
              if (n.toString.equals(value)) "checked" else null
            } value={ n.toString }>
            </input>
            <label for={
              lit.htmlName }>{ n }</label>
          )).flatten
        else {
          <select name={
            lit.htmlName }>
            { formatPossibleValues(lit) }
          </select>
        }
        }</div>
        , html5Type )

      case typ if typ === xsdPrefix + "boolean" =>
        val html5Type = "radio"
        (
        <div class="wrapper"
             title={lit.property.toString()} >
          <label for="yes_radio" id="yes-lbl">Oui</label>
          <input type={html5Type} name={
            lit.htmlName } id="yes_radio"
          checked={toPlainString(value) match {case "true" => "true" ; case _ => null } }
          value="true" ></input>

          <label for="maybe_radio" id="maybe-lbl">?</label>
          <input type={html5Type} name={
            lit.htmlName } id="maybe_radio"
          checked={toPlainString(value) match {case "" => "checked" ; case _ => null } }
          value="" ></input>

          <label for="no_radio" id="no-lbl">Non</label>
          <input type="radio" name={
            lit.htmlName } id="no_radio"
          checked={toPlainString(value) match {case "false" => "false" ; case _ => null } }
          value="false" ></input>
          <div class="toggle"></div>
        </div>
        , html5Type )

      case _ =>
          val html5Type ={
            if( lit.property.toString() . toLowerCase().endsWith("password"))
              "password"
            else
              xsd2html5TnputType(type0)
          }
          // <!-- TODO ? dropzone="copy" -->
          val inputElement =
          <input class={ cssConfig.formInputCSSClass }
          value={
            toPlainString(value)
          } name={ lit.htmlName } type={html5Type}
          step = {xsd2html5Step(type0)}
          placeholder={ placeholder } title={ placeholder } size={
            inputSize.toString()
          } id={ htmlId }
          ></input>
          val input =
            if( lit.widgetType == ShortString ||
                html5Type != "text" )
              inputElement
            else {
              val te : Elem = <textarea>{ toPlainString(value) }</textarea>
              // attributes are thus pasted from <input> to <textarea> :
              te % (inputElement . attributes)
            }
        (input, html5Type )
      }

        Text("\n") ++
        <div class={ cssConfig.formDivInputCSSClass }>
          { addTripleAttributesToXMLElement( input, lit ) }
        </div> ++
        <div class={ cssConfig.formDivEditInputCSSClass }>{
          val scriptUnused = <script>{ Unparsed(s"""
            var input = document.getElementById( "$htmlId" )
            var myCodeMirror = CodeMirror.fromTextArea(input)
            console.log("myCodeMirror " + myCodeMirror)
                """)
          }</script>
          if (showEditButtons &&
                 ! (lit.widgetType == ShortString) &&
                 html5Type == "text" )
              <input class="btn btn-primary" type="button" value="EDIT"
                onClick={ s"""
                  // var input = document.getElementById( "$htmlId" )
                  // var content = input .value
                  $$('#$htmlId') .summernote( {
                    height: 250,      // set editor height
                    minHeight: null,  // set minimum height of editor
                    maxHeight: null,  // set maximum height of editor
                    focus: true      // set focus to editable area after initializing summernote
                    // , codemirror: {}
                  } )
                """ }
                title="Click to edit multiline text in HTML editor">
              </input>
          }
        </div>
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

}
