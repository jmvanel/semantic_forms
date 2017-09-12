package deductions.runtime.views

import java.net.URLEncoder

import deductions.runtime.core.FormModule
import deductions.runtime.services.{ParameterizedSPARQL, SPARQLQueryMaker}
import org.w3.banana.RDF

import scala.xml.NodeSeq
import deductions.runtime.abstract_syntax.ThumbnailInference

trait ResultsDisplay[Rdf <: RDF, DATASET]
extends ThumbnailInference[Rdf, DATASET] {

  self: ParameterizedSPARQL[Rdf, DATASET] =>
    
  import ops._

  /**
   * generate a row of HTML hyperlinks for given list of RDF Node;
   * non TRANSACTIONAL
   * TODO
   * - be able to add HTML at the end of the row, or even in-between
   *   (for issues "Named graph view" : https://github.com/jmvanel/semantic_forms/issues/136 , etc) */
  def displayResults(res0: Iterable[Rdf#Node],
      hrefPrefix: String = config.hrefDisplayPrefix,
      lang: String = "",
      graph: Rdf#Graph,
      sortAnd1rowPerElement:Boolean = false )
  (implicit queryMaker: SPARQLQueryMaker[Rdf] )
  : NodeSeq = {
    val wrappingClass = "row sf-triple-block"
    <div class={wrappingClass} >{
      val res = res0 .toSeq // List
      val uriLabelCouples = res.map(uri => (uri, makeInstanceLabel(uri, graph, lang)))
//      println(s"displayResults: sortAnd1rowPerElement $sortAnd1rowPerElement")

      val uriLabelCouples2 =
        if( sortAnd1rowPerElement )
        uriLabelCouples. sortBy(c => c._2)
        else uriLabelCouples
//        println(s"displayResults: $uriLabelCouples2")
      val columnsFormResults = uriLabelCouples2 .
        map{
            uriLabelCouple => {
          val node = uriLabelCouple._1
          val label = uriLabelCouple._2
          <div class="col col-sm-4 sf-value-block">{
          foldNode(node) (
          uri =>
            makeHyperlinkForURI(
              uri, lang, graph,
              hrefPrefix = hrefPrefix,
              label = label,
              sortAnd1rowPerElement = sortAnd1rowPerElement ) ++
              <div style="font-size:10px; opacity:.8;">{ val uriString = fromUri(uri)
              URLEncoder.encode( s"DROP GRAPH <$uriString>", "utf-8") }</div>
            ,
          bnode =>
            makeHyperlinkForURI(
              bnode, lang, graph,
              hrefPrefix = hrefPrefix,
              label = label,
              sortAnd1rowPerElement = sortAnd1rowPerElement ),
          lit => <div>{ lit.toString() }</div>
          )
          }</div>
        }
        }
        columnsFormResults
    }</div><!-- end of wrapping div displayResults -->
  }

    /** make HTML hyperlink For given URI;
   *  this links to semantic_forms page for displaying this URI;
   * NOTE: this duplicates code in Form2HTMLDisplay.createHTMLResourceReadonlyField() */
//  protected
  def makeHyperlinkForURI( node: Rdf#Node, lang: String, graph: Rdf#Graph,
      hrefPrefix: String = config.hrefDisplayPrefix,
      label: String = "",
      sortAnd1rowPerElement:Boolean = false )
    : NodeSeq = {
    val displayLabel =
      if( label != "" )
          label
        else
          makeInstanceLabel(node, graph, lang)
     val `type` = getClassOrNullURI(node)(graph)
     displayNode(uriNodeToURI(node), hrefPrefix, displayLabel,
         property = nullURI, type_ = `type` )
  }

  /** call createHTMLResourceReadonlyField() in trait Form2HTMLDisplay */
  private def displayNode(uri: Rdf#URI, hrefPrefix: String = config.hrefDisplayPrefix,
		  label: String,
      property: Rdf#URI,
      type_ : Rdf#Node
      ): NodeSeq = {
    val fmod = new FormModule[Rdf#Node,  Rdf#URI ]{
      val nullURI= ops.URI("")
      }
    val resourceEntry = new fmod.ResourceEntry(
      label, "comment", property, new fmod.ResourceValidator(Set()),
      value=uri, true,
      Seq(), label, type_, false,
      thumbnail = getURIimage(uri) )
    def hyperlink =
      if( hrefPrefix != "" &&
          hrefPrefix != config.hrefDisplayPrefix )
    	<a href={
    			createHyperlinkString(hrefPrefix, fromUri(uri), false)
    } class="" title={s"hyperlink to Triples in Graph at URI <$uri>"}>
    { label } </a>
    else NodeSeq.Empty

    hyperlink ++
    createHTMLResourceReadonlyField( resourceEntry, hrefPrefix)
  }
  
}