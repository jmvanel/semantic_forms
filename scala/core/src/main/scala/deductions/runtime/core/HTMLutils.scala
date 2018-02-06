package deductions.runtime.core

import scala.xml.NodeSeq

trait HTMLutils {

  def showHideHTMLOnClick(html: NodeSeq,
        resourceId: String
        ): NodeSeq = {
    val wrapperId = resourceId+"-wrap"
    val buttonId = resourceId+"-button"
    <button id={buttonId} class="showHideButton">...</button> ++
    <span id={wrapperId} style="display: none">{
      // NOTE script to show/Hide HTML On click is hooked in head.html
      html
    }</span>
  }
}