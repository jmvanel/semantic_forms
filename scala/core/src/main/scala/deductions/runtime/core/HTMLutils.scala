package deductions.runtime.core

import scala.xml.NodeSeq
import scala.xml.Elem
import scala.xml.{ Text => XMLText }
import scala.xml.Null
import scala.xml.Attribute

trait HTMLutils {

  /** show or Hide given HTML fragment On Clicking on given button */
  def showHideHTMLOnClick(html: NodeSeq,
        resourceId: String,
        buttonToggle: Elem = <button>...</button>
        ): NodeSeq = {
    val wrapperId = resourceId+"-wrap"
    val buttonId = resourceId+"-button"
    // <button id={buttonId} class="showHideButton">...</button>
    buttonToggle %
      Attribute( None, "id", XMLText(buttonId), Null) %
      Attribute( None, "class", XMLText("showHideButton sf-button"), Null) ++
    <span id={wrapperId} style="display: none">{
      // NOTE script to show/Hide HTML On click is hooked in head.html
      html
    }</span>
  }

/** Same as #showHideHTMLOnClick, but button is generated for Expert Buttons */
  def showHideExpertButtonsOnClick(html: NodeSeq,
      resourceId: String) = {
    showHideHTMLOnClick(html, resourceId,
          <button title={
      s"""Show "expert" buttons: navigate, edit, graph, for value <${resourceId}>"""}
              style="height: 20px"
          >...</button>)
  }
}