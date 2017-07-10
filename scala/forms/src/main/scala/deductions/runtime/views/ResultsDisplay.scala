package deductions.runtime.views

import java.net.URLEncoder

import deductions.runtime.abstract_syntax.FormModule
import deductions.runtime.services.{ParameterizedSPARQL, SPARQLQueryMaker}
import org.w3.banana.RDF

import scala.xml.NodeSeq

trait ResultsDisplay[Rdf <: RDF, DATASET] {

  self: ParameterizedSPARQL[Rdf, DATASET] =>
    
      import ops._

    ///////////// TODO extract all this in ResultsDisplay /////////////////////

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
      val res = res0.toList
      val uriLabelCouples = res.map(uri => (uri, makeInstanceLabel(uri, graph, lang)))
      val uriLabelCouples2 = if( sortAnd1rowPerElement )
        uriLabelCouples. sortBy(c => c._2)
        else uriLabelCouples
      val columnsFormResults = uriLabelCouples2 .
        map(uriLabelCouple => {
          val node = uriLabelCouple._1
          val label = uriLabelCouple._2
          <div class="col col-sm-4 sf-value-block">{
          foldNode(node) (
          node =>
            makeHyperlinkForURI(
              node, lang, graph,
              hrefPrefix = hrefPrefix,
              label = label,
              sortAnd1rowPerElement = sortAnd1rowPerElement ) ++
              <div style="font-size:10px; opacity:.8;">{ val uriString = fromUri(node)
              URLEncoder.encode( s"DROP GRAPH <$uriString>", "utf-8") }</div>
            ,
          node =>
            makeHyperlinkForURI(
              node, lang, graph,
              hrefPrefix = hrefPrefix,
              label = label,
              sortAnd1rowPerElement = sortAnd1rowPerElement ),
          lit => <div>{ lit.toString() }</div>
          )
          }</div>
        })
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
    (implicit queryMaker: SPARQLQueryMaker[Rdf] ): NodeSeq = {
    val displayLabel =
      if( label != "" )
          label
        else
          makeInstanceLabel(node, graph, lang)
     displayNode(uriNodeToURI(node), hrefPrefix, displayLabel,
         property = nullURI, type_ = nullURI )
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
      Seq(), label, type_, false)
    createHTMLResourceReadonlyField( resourceEntry, hrefPrefix)
  }

  /** make HTML hyperlink For given URI;
   *  this links to semantic_forms page for displaying this URI;
   * NOTE: this duplicates code in Form2HTMLDisplay.createHTMLResourceReadonlyField() */
//  private def makeHyperlinkForURIOLD( node: Rdf#Node, lang: String, graph: Rdf#Graph,
//      hrefPrefix: String = hrefDisplayPrefix,
//      label: String = "",
//      sortAnd1rowPerElement:Boolean = false )
//    (implicit queryMaker: SPARQLQueryMaker[Rdf] ): NodeSeq = {
//    val uriString = node.toString
//    val blanknode = !isURI(node)
//    val displayLabel =
//      if( label != "" )
//          label
//        else
//          instanceLabel(node, graph, lang)
//
//    <div title={ node.toString() } class={
//      if( sortAnd1rowPerElement ) "form-row" else "form-value"
//    }> {
//      val css= cssForURI(uriString)
//      def hyperlink =
//                <a href={
//                Form2HTML.createHyperlinkString(hrefPrefix, uriString, blanknode)
//              } class={css}>
//              { displayLabel } </a>
//      val nodeRendering = foldNode(node)(
//                x => hyperlink,
//                x => hyperlink,
//                x => Text( x.toString() ))
//                
//            	val columns_for_URI = queryMaker.columnsForURI( node,
//            	    displayLabel )
////            	println( "displayResults " + node + columns_for_URI )
//            	
//            	nodeRendering ++ columns_for_URI // ++ <br/> 
//    } </div><!-- end of row div -->   	
//  }

  
}