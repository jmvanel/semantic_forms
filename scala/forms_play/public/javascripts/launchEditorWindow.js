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