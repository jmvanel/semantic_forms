package deductions.runtime.html

import scala.xml.NodeSeq
import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.Text
import scala.xml.Unparsed
import java.net.URLEncoder

/** generate HTML from abstract Form for Display (Read only) */
trait Form2HTMLDisplay[NODE, URI <: NODE]
    extends Form2HTMLBase[NODE, URI] {

  def createHTMLiteralReadonlyField(l: fm#LiteralEntry): NodeSeq =
    <xml:group>
      <div class="form-cell-display">{ Unparsed(toPlainString(l.value)) }</div>
      <div>{ if (l.lang != "" && l.lang != "No_language") " > " + l.lang }</div>
    </xml:group>

  def createHTMLResourceReadonlyField(
      r: fm#ResourceEntry,
      hrefPrefix: String): NodeSeq = {
    val stringValue = r.value.toString()
    
    val hyperlinkToObjectURI = 
      <a href={ Form2HTML.createHyperlinkString(hrefPrefix, r.value.toString) }
      title={
        s"""Value ${if (r.value.toString != r.valueLabel) r.value.toString else ""}
              of type ${r.type_.toString()}"""
      } draggable="true"> {
        r.valueLabel
      }</a>
    
    val backLinkButton = (if (stringValue.size > 0) {      
				val title = s""" Reverse links for "${r.label}" "${r.value}" """
				makeBackLinkButton(stringValue, title=title )
      } else new Text(""))
      
    val normalNavigationButton = if (stringValue == "")
      Text("")
    else
      <a href={ stringValue } title={ s"Normal HTTP link to ${r.value}" }
      draggable="true">LINK</a>
    
    // TODO different link when we are on localhost or in production (or use N3.js)
    val link = /*hrefDownloadPrefix + URLEncoder.encode( */ stringValue /*, "utf-8")*/
    val drawGraphLink = 
      <button type="button" class="btn-primary" readonly="yes" title="Draw RDF graph"
    	        onclick={ s"popupgraph( '$link' );" }>
        Draw graph
      </button> 
      // http://localhost:9000/download?url=http%3A%2F%2Fjmvanel.free.fr%2Fjmv.rdf%23me
    
    Seq(
      hyperlinkToObjectURI,  
      Text(" "),
      backLinkButton,
      normalNavigationButton,
      drawGraphLink)
  }

  def createHTMLBlankNodeReadonlyField(
    r: fm#BlankNodeEntry,
    hrefPrefix: String) =
    <a href={ Form2HTML.createHyperlinkString(hrefPrefix, r.value.toString, true) }>{
      r.valueLabel
    }</a>
            
}