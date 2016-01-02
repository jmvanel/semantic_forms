package deductions.runtime.js

import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom
import org.scalajs.dom.html
import scala.scalajs.js.JSApp
import scala.scalajs.js
import js.Dynamic.literal
import org.scalajs.dom.raw.BeforeUnloadEvent

@JSExport
object PopupEditor extends JSApp {

  @JSExport
  def main() = ()

  @JSExport
  def launchEditorWindow(elem: html.Input): Unit = {
    val popupWindow = dom.window.open("", "Edit_Markdown_text_for_semantic_forms",
      "height=500,width=500,resizable=yes,modal=yes")
    val closeButton = popupWindow.document.createElement("button").
      asInstanceOf[html.Button]
    closeButton.innerHTML = "SAVE"
    closeButton.textContent = "SAVE"
    // TODO "DISMISS" button

    val editorDiv = popupWindow.document.createElement("div")
    val options = // Map
      literal {
        "editor" -> editorDiv;
        "class" -> "pen";
        "list" -> // editor menu list
          js.Array("insertimage", "blockquote", "h2", "h3", "p", "code",
            "insertorderedlist", "insertunorderedlist", "inserthorizontalrule",
            "indent", "outdent", "bold", "italic", "underline", "createlink")
      }
    
//    val document = js.Dynamic.global.document
//    val playground = document.getElementById("playground")

    // ochrons 15:16 as the docs say, 
    //val today = js.Dynamic.newInstance(js.Dynamic.global.Date)()
    val pen = js.Dynamic.global.Pen
    val editor = js.Dynamic.newInstance(pen)(options)
    //  val editor = new Pen( options )
    
    popupWindow.document.body.appendChild(closeButton)
    popupWindow.document.body.appendChild(editorDiv)

    editorDiv.innerHTML = elem.value
    if (elem.value == "")
      editorDiv.innerHTML = "?"

    // to avoid message "data you have entered may not be saved."
    dom.window.onbeforeunload = (_:BeforeUnloadEvent) => {
      dom.console.log( "popupWindow.onbeforeunload")
    }

    def onClose() = {
      dom.console.log("popupWindow.onunload")
      val md = editor.toMd().toString() // return a markdown string
      if (md != elem.value) {
        dom.console.log("popupWindow.onunload: saving because " + md +
          " != " + elem.value)
        elem.value = md
      } else {
        dom.console.log("popupWindow.onunload: nothing to save")
      }
      popupWindow.document.body.innerHTML = ""
    }

    closeButton.onclick = ( _: dom.MouseEvent ) => {
      onClose(); popupWindow.close()
    }
  }

}
