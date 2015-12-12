package deductions.runtime.html

trait JavaScript {

  // TODO write in Scala.JS & compile
  // See https://github.com/sofish/pen
  val localJS =
    <script type="text/javascript" async="true" src="https://rawgit.com/sofish/pen/master/src/pen.js"></script> ++
      <script type="text/javascript" async="true" src="https://rawgit.com/sofish/pen/master/src/markdown.js"></script> ++
      <script type="text/javascript" async="true">
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
      </script>
}