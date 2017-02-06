package deductions.runtime.html

import scala.io.Source
import scala.xml.Node

/**
 * NOTE: most of the JavaScript is in ../forms_play/public/javascripts/ ;
 *  all this will become Scala.JS :)
 */
trait JavaScript {

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
      <script type="text/javascript" async="true" src="https://rawgit.com/sofish/pen/master/src/markdown.js"></script>


}