package deductions.runtime.services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.Elem
import org.w3.banana.RDF
import org.w3.banana.SparqlOpsModule
import org.w3.banana.TryW
import org.w3.banana.syntax._
import org.w3.banana.Transactor
import org.w3.banana.RDFOpsModule
import org.w3.banana.RDFOps

import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.html.Form2HTML
import deductions.runtime.abstract_syntax.PreferredLanguageLiteral
import deductions.runtime.abstract_syntax.InstanceLabelsInferenceMemory
import deductions.runtime.html.CSS

import scala.xml.NodeSeq
import scala.xml.Text
import scala.util.Try


trait SPARQLQueryMaker[Rdf <: RDF] {
  def makeQueryString(search: String): String
  def variables = Seq("thing")
    /** overridable function for adding columns in response */
  def columnsForURI( node: Rdf#Node, label: String): NodeSeq = Text("")
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
    with Configuration
//    with CSS
    {
  import ops._
  import sparqlOps._
  import rdfStore.transactorSyntax._
  import rdfStore.sparqlEngineSyntax._

  /**
   * Generic SPARQL SELECT with single result columns (must be named "thing"),
   * generate a column of HTML hyperlinks for given search string;
   *  search and display results as an XHTML element
   *  transactional
   */
  def search(search: String, hrefPrefix: String = "",
             lang: String = "")(implicit queryMaker: SPARQLQueryMaker[Rdf] ): Future[NodeSeq] = {
    println(s"search: starting TRANSACTION for dataset $dataset")
    val elem0 = dataset.rw({
    	val uris = search_onlyNT(search)
//      println(s"after search_only uris ${uris}")
    	val graph: Rdf#Graph = allNamedGraph
//      println(s"displayResults : 1" + graph  )
      val elems =
        <div class={css.tableCSSClasses.formRootCSSClass}> {
    	    css.localCSS ++
        uris.map(
        u => displayResults(u.toIterable, hrefPrefix, lang, graph, true)) . get
    	}</div>
      elems
    })
    println(s"search: leaving TRANSACTION for dataset $dataset")
    val elem = elem0.get
//    elem.asFuture
    Future.successful( elem )
  }

  /**
   * Generic SPARQL SELECT Search with multiple result columns;
   * search and display results as an XHTML element;
   * generate rows of HTML hyperlinks for given search string;
   * transactional
   */
  def search2(search: String, hrefPrefix: String = "",
              lang: String = "")(implicit queryMaker: SPARQLQueryMaker[Rdf] )
  = {
    val uris = search_only2(search)
//    println(s"after search_only uris $uris")
    val elem0 =
      dataset.rw({
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

  /**
   * generate a row of HTML hyperlinks for given list of RDF Node;
   *  non TRANSACTIONAL
   */
  private def displayResults(res0: Iterable[Rdf#Node],
      hrefPrefix: String = hrefDisplayPrefix,
      lang: String = "",
      graph: Rdf#Graph,
      sortAnd1rowPerElement:Boolean = false )
  (implicit queryMaker: SPARQLQueryMaker[Rdf] )
  : NodeSeq = {
    val wrappingClass = ""
    <div class={wrappingClass} >{
      val res = res0.toList
//      println(s"displayResults:\n${res.mkString("\n")}")
      val uriLabelCouples = res.map(uri => (uri, instanceLabel(uri, graph, lang)))
      val uriLabelCouples2 = if( sortAnd1rowPerElement )
        uriLabelCouples. sortBy(c => c._2)
        else uriLabelCouples
      val columnsFormResults = uriLabelCouples2 .
        map(uriLabelCouple => {
          val node = uriLabelCouple._1
          val uriString = node.toString
          val blanknode = !isURI(node)
          <div title={ node.toString() } class={
            if( sortAnd1rowPerElement ) "form-row" else "form-value"
              }> {
              def hyperlink = <a href={
                Form2HTML.createHyperlinkString(hrefPrefix, uriString, blanknode)
              } class="form-value">
                                { uriLabelCouple._2 } </a>
              val nodeRendering = foldNode(node)(
                x => hyperlink,
                x => hyperlink,
                x => Text( x.toString() ))
                
            	val columns_for_URI = queryMaker.columnsForURI( node, instanceLabel(node, graph, lang))
//            	println( "displayResults " + node + columns_for_URI )
            	
            	nodeRendering ++ columns_for_URI // ++ <br/>
            } </div><!-- end of row div -->
        })
        columnsFormResults 
    }</div><!-- end of wrapping div -->
  }

  /** make HTML hyperlink For given URI;
   *  this links to smeantic_forms page for diaplaying this URI */
  def makeHyperlinkForURI( node: Rdf#Node, lang: String, graph: Rdf#Graph,
      hrefPrefix: String = hrefDisplayPrefix,
      label: String = "",
      sortAnd1rowPerElement:Boolean = false )
    (implicit queryMaker: SPARQLQueryMaker[Rdf] ): NodeSeq = {
    val uriString = node.toString
    val blanknode = !isURI(node)
    val displayLabel =
      if( label != "" )
          label
        else
          instanceLabel(node, graph, lang)
    <div title={ node.toString() } class={
            if( sortAnd1rowPerElement ) "form-row" else "form-value"
              }> {
              def hyperlink =
                <a href={
                Form2HTML.createHyperlinkString(hrefPrefix, uriString, blanknode)
              } class="form-value">
              { displayLabel } </a>
              val nodeRendering = foldNode(node)(
                x => hyperlink,
                x => hyperlink,
                x => Text( x.toString() ))
                
            	val columns_for_URI = queryMaker.columnsForURI( node,
            	    displayLabel
//            	    instanceLabel(node, graph, lang)
            	    )
//            	println( "displayResults " + node + columns_for_URI )
            	
            	nodeRendering ++ columns_for_URI // ++ <br/> 
    } </div><!-- end of row div -->   	
  }

  /**
   * TRANSACTIONAL
   *
   * CAUTION: It is of particular importance to note that
   * one should never use an Iterator after calling a method on it;
   * cf http://stackoverflow.com/questions/18420995/scala-iterator-one-should-never-use-an-iterator-after-calling-a-method-on-it
   */
  private def search_only(search: String)
  (implicit queryMaker: SPARQLQueryMaker[Rdf] ): Future[Iterator[Rdf#Node]] = {
    val transaction =
      dataset.r({
    	  search_onlyNT(search)
      })
    val tryIteratorRdfNode = transaction.flatMap { identity }
    println( s"after search_only(search tryIteratorRdfNode $tryIteratorRdfNode" )
    tryIteratorRdfNode.asFuture
  }
  
  /** non TRANSACTIONAL */
  private def search_onlyNT(search: String)
  (implicit queryMaker: SPARQLQueryMaker[Rdf] ): Try[Iterator[Rdf#Node]] = {
    val queryString = queryMaker.makeQueryString(search)
    println( s"search_only(search $search" )
        println(s"search_only: starting TRANSACTION for dataset $dataset")
        val result = for {
          query <- parseSelect(queryString)
          solutions <- dataset.executeSelect(query, Map())
        } yield {
          solutions.toIterable.map {
            row =>
              row("thing") getOrElse sys.error(s"search_only($search) : no ?thing in row")
          }
        }
    println( s"after search_only(search $search" )
    result
  }

  /** with result variables specified; transactional */
  private def search_only2(search: String)
  (implicit queryMaker: SPARQLQueryMaker[Rdf] ): List[Seq[Rdf#Node]] = {
    val queryString = queryMaker.makeQueryString(search)
	  println( s"search_only2( search $search" )
    sparqlSelectQueryVariables(queryString, queryMaker.variables )
  }

}
