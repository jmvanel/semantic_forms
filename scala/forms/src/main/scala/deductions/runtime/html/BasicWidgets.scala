package deductions.runtime.html

import scala.xml.Elem

trait BasicWidgets {

  def makeBackLinkButton(uri: String, title: String = ""): Elem = {
    // format: OFF
    val tit = if (title == "") s" Reverse links for &lt;$uri&gt;" else title
    <button type="button" 
    		class="btn-primary" readonly="yes" title={ tit } data-value="$uri" onclick={ s"backlinks( '$uri' )" } id={ s"BACK-$uri" }>
      ? --&gt; o
    </button>
  }
}