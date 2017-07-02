package deductions.runtime.html

import scala.io.Source
import scala.xml.Node

/**
 * NOTE: most of the JavaScript is in ../forms_play/public/javascripts/ ;
 * See forms/src/main/resources/deductions/runtime/html/head.html
 *
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
    val s = scala.xml.Unparsed(
      result +
        """
        ////////////////////////////
        deductions.runtime.js.PopupEditor().main();
        function launchEditorWindow(input){
          deductions.runtime.js.PopupEditor().launchEditorWindow(input); };

        GPS2.listenToSubmitEventFillGeoCoordinates();
        console.log('Called listenToSubmitEventFillGeoCoordinates');
      """)
      <script type="text/javascript">{s}</script>
  }

  // See https://github.com/sofish/pen
  lazy val localJS =
      javascriptCodeScalaJS

}