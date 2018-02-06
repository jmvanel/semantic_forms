package deductions.runtime.core

import scala.xml.NodeSeq
import scala.xml.Elem
import scala.xml.{ Text => XMLText }
import scala.xml.Null
import scala.xml.Attribute

trait HTMLutils {

  def showHideHTMLOnClick(html: NodeSeq,
        resourceId: String,
        buttonToggle: Elem = <button>...</button>
        ): NodeSeq = {
    val wrapperId = resourceId+"-wrap"
    val buttonId = resourceId+"-button"
    // <button id={buttonId} class="showHideButton">...</button>
    buttonToggle % Attribute(
        None, "id", XMLText(buttonId), Null) % Attribute(
        None, "class", XMLText("showHideButton"), Null) ++
    <span id={wrapperId} style="display: none">{
      // NOTE script to show/Hide HTML On click is hooked in head.html
      html
    }</span>
  }
}