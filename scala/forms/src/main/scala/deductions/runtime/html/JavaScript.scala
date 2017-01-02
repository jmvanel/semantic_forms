package deductions.runtime.html

import scala.io.Source
import scala.xml.Node

/**
 * NOTE: most of the JavaScript is in ../forms_play/public/javascripts/ ;
 *  all this will become Scala.JS :)
 */
trait JavaScript {

  /** native javascript Code (written now in Scala.JS) */
  private lazy val javascriptCode_native: Node =
    // Prevent Scala from escaping double quotes in XML unnecessarily
    scala.xml.Unparsed(
      """
function launchEditorWindow( elem /* input */ ) {  
  var popupWindow = window.open('', 'Edit_Markdown_text_for_semantic_forms',
    'height=500,width=500,resizable=yes,modal=yes');
  var closeButton = popupWindow.document.createElement( "button" );
  closeButton.value = "SAVE";
  closeButton.name = "SAVE";
  closeButton.innerHTML = "SAVE";
  closeButton.textContent = "SAVE";

  var exitButton = popupWindow.document.createElement( "button" );
  exitButton.textContent = "DISMISS";

  var editorDiv = popupWindow.document.createElement( "div" ); 
  popupWindow.document.body.appendChild( closeButton );
  popupWindow.document.body.appendChild( exitButton );
  popupWindow.document.body.appendChild( editorDiv );
  closeButton.onclick = function() { onClose(); popupWindow.close(); };
  exitButton.onclick = function() { popupWindow.close(); };

  var options = {
    editor: editorDiv,
    class: 'pen',
    list: // editor menu list
    [ 'insertimage', 'blockquote', 'h2', 'h3', 'p', 'code',
      'insertorderedlist', 'insertunorderedlist', 'inserthorizontalrule',
      'indent', 'outdent', 'bold', 'italic', 'underline', 'createlink' ]
  }
  editorDiv.innerHTML = elem.value;
  if( elem.value == "" )
    editorDiv.innerHTML = "?";

  var editor = new Pen( options );
  // to avoid message "data you have entered may not be saved."
  window.onbeforeunload = function(e) { console.log( "popupWindow.onbeforeunload"); return undefined; };
  function onClose() {
    console.log( "popupWindow.onunload");
    var md = editor.toMd(); // return a markdown string
    if( md != elem.value ) {
      console.log( "popupWindow.onunload: saving because " + md +
        " != " + elem.value );
      elem.value = md;
    } else {
      console.log( "popupWindow.onunload: nothing to save");
    }
    popupWindow.document.body.innerHTML = "";
  };
};
  """)

  /**
   * NOTE: with Scala 2.11.7, it is not possible to declare this private:
   *  that triggers an AbstractMethodException
   */
  lazy val javascriptCode: Node = javascriptCode_native

  /** compiled from Scala.js */
  lazy private val javascriptCodeScalaJS: Node = {
    val compiledScalaJS =
      getClass.getResource("/deductions/runtime/js/forms_js-fastopt.js")
    val source = if (compiledScalaJS != null)
      Source.fromURL(compiledScalaJS)
    else Source.fromString("")
    val result = source.mkString
    source.close()
    scala.xml.Unparsed(
      result +
        """
        ////////////////////////////
        deductions.runtime.js.PopupEditor().main();
        function launchEditorWindow(input){
          deductions.runtime.js.PopupEditor().launchEditorWindow(input); };
      """)
  }

  // See https://github.com/sofish/pen
  lazy val localJS =
    <script type="text/javascript" async="true" src="https://rawgit.com/sofish/pen/master/src/pen.js"></script> ++
      <script type="text/javascript" async="true" src="https://rawgit.com/sofish/pen/master/src/markdown.js"></script> ++
      <script type="text/javascript" async="true">{ javascriptCode }</script>

}