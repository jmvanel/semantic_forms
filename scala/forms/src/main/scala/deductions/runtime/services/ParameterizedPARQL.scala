package deductions.runtime.services

import scala.concurrent.Future
import scala.xml.Elem
import scala.xml.NodeSeq
import scala.xml.Text

import org.w3.banana.RDF
import org.w3.banana.TryW

import deductions.runtime.abstract_syntax.FormModule
import deductions.runtime.abstract_syntax.InstanceLabelsInferenceMemory
import deductions.runtime.abstract_syntax.PreferredLanguageLiteral
import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.html.Form2HTML
import deductions.runtime.html.Form2HTMLDisplay
import java.net.URLEncoder


trait SPARQLQueryMaker[Rdf <: RDF] {
  // TODO : search: String*
  def makeQueryString(search: String*): String
  def variables = Seq("thing")
    /** overridable function for adding columns in response */
  def columnsForURI( node: Rdf#Node, label: String): NodeSeq = Text("")

  def prepareSearchString(search: String) = {
    search.trim().replace("'", """\'""")
  }
}

/**
 * Generic SPARQL SELECT Search with single parameter,
 * and single return URI value,
 *  and showing in HTML a column of hyperlinked results with instance Labels
 */
abstract trait ParameterizedSPARQL[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with InstanceLabelsInferenceMemory[Rdf, DATASET]
    with PreferredLanguageLiteral[Rdf]
    with SPARQLHelpers[Rdf, DATASET]
    with Form2HTMLDisplay[Rdf#Node, Rdf#URI] {

  import config._
  import ops._
  import rdfStore.transactorSyntax._

  /**
   * Generic SPARQL SELECT with single result columns (must be named "thing"),
   * generate a column of HTML hyperlinks for given search string;
   * search and display results as an XHTML element
   * transactional
   * @param hrefPrefix URL prefix for creating hyperlinks ((URI of each query result is concatenated)
   *  
   * TODO
   * - displayResults should be a function argument
   * - result2 is very similar!
   */
  def search(hrefPrefix: String, 
             lang: String,
             search: String*
             )(implicit queryMaker: SPARQLQueryMaker[Rdf] ): Future[NodeSeq] = {
    val elem0 = rdfStore.rw( dataset, {
      println(s"search 1: starting TRANSACTION for dataset $dataset")
    	val uris = search_onlyNT(search :_* )
    	val graph: Rdf#Graph = allNamedGraph
      val elems =
        <div class={css.tableCSSClasses.formRootCSSClass}> {
    	    css.localCSS ++
        uris.map(
        u => displayResults(u.toIterable, hrefPrefix, lang, graph, true)) // . get
    	}</div>
      elems
    })
    println(s"search: leaving TRANSACTION for dataset $dataset")
    val elem = elem0.get
    Future.successful( elem )
  }

  /**
   * Generic SPARQL SELECT Search with multiple result columns;
   * search and display results as an XHTML element;
   * generate rows of HTML hyperlinks for given search string;
   * transactional
   * @param hrefPrefix URL prefix for creating hyperlinks ((URI of each query result is concatenated)
   */
  def search2(search: String, hrefPrefix: String = "",
              lang: String = "")(implicit queryMaker: SPARQLQueryMaker[Rdf] ): Elem
  = {
    val uris = search_only2(search)
    val elem0 =
      rdfStore.rw( dataset, {
    	  val graph: Rdf#Graph = allNamedGraph
        <div class={css.tableCSSClasses.formRootCSSClass}> {
    	    css.localCSS ++
    	    uris.map(
            // create table like HTML
          u => displayResults(u.toIterable, hrefPrefix, lang, graph))
    	  }</div>
      })
    val elem = elem0.get
    elem
  }

  ///////////// TODO extract all this in ResultsDisplay /////////////////////

  /**
   * generate a row of HTML hyperlinks for given list of RDF Node;
   * non TRANSACTIONAL
   * TODO
   * - be able to add HTML at the end of the row, or even in-between
   *   (for issues "Named graph view" : https://github.com/jmvanel/semantic_forms/issues/136 , etc) */
  private def displayResults(res0: Iterable[Rdf#Node],
      hrefPrefix: String = hrefDisplayPrefix,
      lang: String = "",
      graph: Rdf#Graph,
      sortAnd1rowPerElement:Boolean = false )
  (implicit queryMaker: SPARQLQueryMaker[Rdf] )
  : NodeSeq = {
    val wrappingClass = "row sf-triple-block"
    <div class={wrappingClass} >{
      val res = res0.toList
      val uriLabelCouples = res.map(uri => (uri, instanceLabel(uri, graph, lang)))
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
  protected def makeHyperlinkForURI( node: Rdf#Node, lang: String, graph: Rdf#Graph,
      hrefPrefix: String = hrefDisplayPrefix,
      label: String = "",
      sortAnd1rowPerElement:Boolean = false )
    (implicit queryMaker: SPARQLQueryMaker[Rdf] ): NodeSeq = {
    val displayLabel =
      if( label != "" )
          label
        else
          instanceLabel(node, graph, lang)
     displayNode(uriNodeToURI(node), hrefPrefix, displayLabel,
         property = nullURI, type_ = nullURI )
  }

  /** call createHTMLResourceReadonlyField() in trait Form2HTMLDisplay */
  private def displayNode(uri: Rdf#URI, hrefPrefix: String = hrefDisplayPrefix,
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

  /**
   * TRANSACTIONAL
   *
   * CAUTION: It is of particular importance to note that
   * one should never use an Iterator after calling a method on it;
   * cf http://stackoverflow.com/questions/18420995/scala-iterator-one-should-never-use-an-iterator-after-calling-a-method-on-it
   */
  private def search_only(search: String*)
  (implicit queryMaker: SPARQLQueryMaker[Rdf] )
  // : Future[Iterator[Rdf#Node]]
  = {
    println(s"search 2: starting TRANSACTION for dataset $dataset")
    val transaction =
      rdfStore.r( dataset, {
    	  search_onlyNT(search :_* )
      })
    val tryIteratorRdfNode = transaction // .flatMap { identity }
    println( s"after search_only(search tryIteratorRdfNode $tryIteratorRdfNode" )
    tryIteratorRdfNode.asFuture
  }
  
  private def search_onlyNT(search: String*)
  (implicit queryMaker: SPARQLQueryMaker[Rdf] )
  // : Try[Iterator[Rdf#Node]] 
  = {
    val queryString = queryMaker.makeQueryString(search :_* )
    logger.debug( s"search_onlyNT(search='$search') \n$queryString \n\tdataset Class ${dataset.getClass().getName}" )
    sparqlSelectQueryVariablesNT(queryString, Seq("thing") )
  }

  /** with result variables specified; transactional */
  private def search_only2(search: String)
  (implicit queryMaker: SPARQLQueryMaker[Rdf] ): List[Seq[Rdf#Node]] = {
    val queryString = queryMaker.makeQueryString(search)
	  println( s"search_only2( search $search" )
    sparqlSelectQueryVariables(queryString, queryMaker.variables )
  }

}
