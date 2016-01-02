package deductions.runtime.html

trait JavaScript {

  /** TODO write in Scala.JS & compile */
  private val javascriptCode =
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
  // TODO "DISMISS" button

  var editorDiv = popupWindow.document.createElement( "div" ); 
  popupWindow.document.body.appendChild( closeButton );
  popupWindow.document.body.appendChild( editorDiv );
  closeButton.onclick = function() { onClose(); popupWindow.close(); };

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

  // See https://github.com/sofish/pen
  lazy val localJS =
    <script type="text/javascript" async="true" src="https://rawgit.com/sofish/pen/master/src/pen.js"></script> ++
      <script type="text/javascript" async="true" src="https://rawgit.com/sofish/pen/master/src/markdown.js"></script> ++
      <script type="text/javascript" async="true">{ javascriptCode }</script>

  /*
function launchEditorWindow(elem/* :input */) {{
  var popupWindow = window.open('', 'Edit Markdown text for semantic_forms',
    'height=500, width=500');
  var options = {{
    editor: popupWindow.document.body,
    class: 'pen',
    list: // editor menu list
    [ 'insertimage', 'blockquote', 'h2', 'h3', 'p', 'code', 'insertorderedlist', 'insertunorderedlist', 'inserthorizontalrule',
      'indent', 'outdent', 'bold', 'italic', 'underline', 'createlink' ]
  }}
  popupWindow.document.body.innerHTML = elem.value
  var editor = new Pen( options );
  popupWindow.onunload = function() {{
    elem.value = editor.toMd(); // return a markdown string
    return void(0)
  }};
        }}
       */
}