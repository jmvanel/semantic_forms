package deductions.runtime.views

import java.net.URLEncoder

import deductions.runtime.core.FormModule
import deductions.runtime.services.{ParameterizedSPARQL, SPARQLQueryMaker}
import org.w3.banana.RDF

import scala.xml.NodeSeq
import deductions.runtime.abstract_syntax.ThumbnailInference
import deductions.runtime.utils.FormModuleBanana
import deductions.runtime.core.HTTPrequest
import scala.util.Try

/** Results Display, typically SPARQL SELECT */
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
      sortAnd1rowPerElement:Boolean = false,
      request: HTTPrequest = HTTPrequest() )
  : NodeSeq = {
    val wrappingClass = "row sf-triple-block"
    val tryResult = Try{
    <div class={wrappingClass} >{
      val res = res0 .toSeq
      val uriLabelCouples = res.map(uri => (uri, makeInstanceLabelFuture(uri, graph, lang)))
//      val uriLabelCouples = res.map(uri => (uri, makeInstanceLabel(uri, graph, lang)))
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
          Seq( <div class="sf-value-block">{
          foldNode(node) (
          uri =>
            makeHyperlinkForURI(
              uri, lang, graph,
              hrefPrefix = hrefPrefix,
              label = label,
              request,
              sortAnd1rowPerElement = sortAnd1rowPerElement ) ++
            separatorSpan ++
            makeHyperlinkForURIBriefly(
              getClassOrNullURI(uri)(allNamedGraph), lang )
//              ++
//              <div style="font-size:10px; opacity:.8;">{ val uriString = fromUri(uri)
//              URLEncoder.encode( s"DROP GRAPH <$uriString>", "utf-8") }</div>
            ,
          bnode =>
            makeHyperlinkForURI(
              bnode, lang, graph,
              hrefPrefix = hrefPrefix,
              label = label,
              sortAnd1rowPerElement = sortAnd1rowPerElement ),
          lit => <div>{ lit.toString() }</div>
          )
          }</div> , separatorTriple )
        }
        }
        columnsFormResults
    }</div><!-- end of wrapping div displayResults -->
    }
    tryResult.getOrElse{
      val mess = s"ERROR in displayResults(): $tryResult"
      logger.error(mess)
      <div>{ mess }</div>
    }
  }

  val separatorSpan = <span>&#160;&#160;</span>
  val separatorTriple = " | "
  
  /** make HTML hyperlink For given URI, with bells and whistles;
   *  link to semantic_forms page for displaying this URI;
   * NOTE: this reuses code in Form2HTMLDisplay.createHTMLResourceReadonlyField()
   * 
   * NON transactional, needs Read transaction */
  def makeHyperlinkForURI( node: Rdf#Node, lang: String, graph: Rdf#Graph = allNamedGraph,
      hrefPrefix: String = config.hrefDisplayPrefix,
      label: String = "",
      request: HTTPrequest = HTTPrequest(),
      sortAnd1rowPerElement:Boolean = false )
    : NodeSeq = {
    val displayLabel =
      if( label != "" )
          label
        else
          makeInstanceLabelFuture(node, graph, lang)
     val `type` = getClassOrNullURI(node)(graph)
     displayNode(uriNodeToURI(node), hrefPrefix, displayLabel,
         property = nullURI, type_ = `type`, request)
  }

  /** make HTML Hyperlink For URI, short, no expert stuff */
  def makeHyperlinkForURIBriefly(
      node: Rdf#Node,
      lang: String,
      label: String = "",
      graph: Rdf#Graph = allNamedGraph
  ) : NodeSeq = {
    val displayLabel =
      if( label != "" )
          label
        else
          makeInstanceLabelFuture(node, graph, lang)
          // instanceLabelFromTDB(node, lang)
          // makeInstanceLabel(node, graph, lang)
     val `type` = getClassOrNullURI(node)(graph)
     displayNodeBriefly(uriNodeToURI(node), displayLabel,
         property = nullURI, type_ = `type` )
  }

  /** make HTML hyperlink For given URI;
   *  this links to semantic_forms page for displaying this URI;
   * NOTE: this reuses code in Form2HTMLDisplay.createHTMLResourceReadonlyField()
   * 
   * transactional, needs no transaction */
  def makeHyperlinkForURItr( node: Rdf#Node, lang: String, graph: Rdf#Graph = allNamedGraph,
      hrefPrefix: String = config.hrefDisplayPrefix,
      label: String = "",
      sortAnd1rowPerElement:Boolean = false )
    : NodeSeq = {
    		wrapInTransaction{
    			makeHyperlinkForURI( node, lang, graph,
    					hrefPrefix,
    					label,
    					sortAnd1rowPerElement=sortAnd1rowPerElement)
    		} . getOrElse(<div/>)
  }

  /** display given URI with bells and whistles,
   *  implementation: call createHTMLResourceReadonlyField() from trait Form2HTMLDisplay */
  private def displayNode(uri: Rdf#URI,
      hrefPrefix: String = config.hrefDisplayPrefix,
      label: String,
      property: Rdf#URI,
      type_ : Rdf#Node,
      request: HTTPrequest = HTTPrequest()
      ): NodeSeq = {
    if( uri != nullURI ) {
      val resourceEntry = makeResourceEntry(uri, label, property, type_)

      def hyperlink =
        if( hrefPrefix != "" &&
            hrefPrefix != config.hrefDisplayPrefix )
          <a href={
             createHyperlinkString(hrefPrefix, fromUri(uri), false)
           } class="" title={s"hyperlink to Triples in Graph at URI <$uri>"}>
           { label } </a>
        else NodeSeq.Empty

      hyperlink ++
      createHTMLResourceReadonlyField( resourceEntry, request )
    } else
      <span>null URI</span>
  }

  /** create HTML Resource Readonly Field, just hyperlink to URI and thumbnail */
  private def displayNodeBriefly(uri: Rdf#URI,
      label: String,
      property: Rdf#URI,
      type_ : Rdf#Node
      ): NodeSeq = {
        createHTMLResourceReadonlyFieldBriefly(
            makeResourceEntry(uri, label, property, type_ ) )
  }

  private def makeResourceEntry(uri: Rdf#URI,
      label: String,
      property: Rdf#URI,
      type_ : Rdf#Node
      ) = {
    val ops1 = ops
    val fmod = new FormModule[Rdf#Node,  Rdf#URI ]
                   with FormModuleBanana[Rdf] {
      val ops = ops1
        val nullURI= ops.URI("")
      }
      val types = getClasses(uri)(allNamedGraph)
      // println(s"==== displayNode: types: $types")
       new fmod.ResourceEntry(
        valueLabel=label,
        property=property,
        value=uri,
        thumbnail = getURIimage(uri),
        type_ = types,
        isClass = containsClassType(types)
          // isClass(uri)
        )
  }

}