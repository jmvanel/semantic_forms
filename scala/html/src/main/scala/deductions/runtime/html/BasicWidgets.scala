package deductions.runtime.html

import java.net.URLEncoder

import deductions.runtime.utils.RDFPrefixesInterface

import scala.xml.Elem

/** GUI integration: rdfviewer, ... */
trait BasicWidgets extends RDFPrefixesInterface {

  def makeBackLinkButton(uri: String, title: String = ""): Elem = {
    // format: OFF
    val tit = if (title == "") s" Reverse links for &lt;$uri&gt;" else title
    <button type="button" 
    		class="btn btn-info btn-xs" readonly="yes" title={ tit } data-value={s"$uri"} onclick={ s"backlinks( '$uri' )" } id={ s"BACK-$uri" }>
      <i class="glyphicon glyphicon-search"></i>
    </button>
  }
  
  def makeDrawGraphLink( uri: String,
      toolURLprefix: String="/assets/rdfviewer/rdfviewer.html?url=",
      toolname: String="RDF Viewer",
      imgWidth:Int=15): Elem = {

    // TODO different link when we are on localhost (transmit RDF String then) or in production (or use N3.js
    // http://localhost:9000/download?url=http%3A%2F%2Fjmvanel.free.fr%2Fjmv.rdf%23me
    val link = /*hrefDownloadPrefix + */ URLEncoder.encode( uri, "utf-8")

//      <button type="button" class="btn-primary" readonly="yes" title="Draw RDF graph"
//    	        onclick={ s"popupgraph( '$link' );" }>
//    	<form action={ s"/assets/rdfviewer/rdfviewer.html?url=$link" }>
//  		<input type="submit" class="btn-primary" readonly="yes" title="Draw RDF graph"
//    	         value="Draw graph"></input> 
//    	</form >

    if( uri != "" )
    <a class="btn btn-default btn-xs" href={ s"$toolURLprefix$link" }
    title={s"Draw RDF graph with $toolname for $uri"}
    target="_blank">
			<img width={imgWidth.toString()} border="0" src="https://www.w3.org/RDF/icons/rdf_flyer.svg"
           alt="RDF Resource Description Framework Flyer Icon"/>
    </a>
    else <div></div>
  }
  
  /** make link to  WebVOWL
   *  TODO: paste of preceding function !!!!!!!!!!!!! */
  def makeDrawGraphLinkVOWL( uri: String,
      toolURLprefix: String="http://visualdataweb.de/webvowl/#iri=",
      toolname: String="Web VOWL",
      imgWidth:Int=15): Elem = {

    val link = URLEncoder.encode( uri, "utf-8")

    if( uri != "" )
    <a class="btn btn-default" href={ s"$toolURLprefix$link" }
    title={s"Draw VOWL graph with $toolname for $uri"}
    target="_blank">
			<img width={imgWidth.toString()} border="0" src="https://www.w3.org/RDF/icons/rdf_flyer.svg"
           alt="Web VOWL"/>
    </a>
    else <div></div>
  }
}