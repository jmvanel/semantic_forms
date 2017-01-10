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
import deductions.runtime.html.CSS
import deductions.runtime.html.Form2HTML
import deductions.runtime.html.Form2HTMLDisplay


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
    with CSS
    with Form2HTMLDisplay[Rdf#Node, Rdf#URI] {

  import config._
  import ops._
  import rdfStore.transactorSyntax._

  /**
   * Generic SPARQL SELECT with single result columns (must be named "thing"),
   * generate a column of HTML hyperlinks for given search string;
   *  search and display results as an XHTML element
   *  transactional
   */
  def search(hrefPrefix: String, 
             lang: String,
             search: String*
             )(implicit queryMaker: SPARQLQueryMaker[Rdf] ): Future[NodeSeq] = {
    val elem0 = dataset.rw({
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
   */
  def search2(search: String, hrefPrefix: String = "",
              lang: String = "")(implicit queryMaker: SPARQLQueryMaker[Rdf] ): Elem
  = {
    val uris = search_only2(search)
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
   *  non TRANSACTIONAL */
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
          makeHyperlinkForURI(
              node, lang, graph,
              hrefPrefix = hrefPrefix,
              label = uriLabelCouple._2,
              sortAnd1rowPerElement = sortAnd1rowPerElement )
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
  private def makeHyperlinkForURIOLD( node: Rdf#Node, lang: String, graph: Rdf#Graph,
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
      val css= cssForURI(uriString)
      def hyperlink =
                <a href={
                Form2HTML.createHyperlinkString(hrefPrefix, uriString, blanknode)
              } class={css}>
              { displayLabel } </a>
      val nodeRendering = foldNode(node)(
                x => hyperlink,
                x => hyperlink,
                x => Text( x.toString() ))
                
            	val columns_for_URI = queryMaker.columnsForURI( node,
            	    displayLabel )
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
  private def search_only(search: String*)
  (implicit queryMaker: SPARQLQueryMaker[Rdf] )
  // : Future[Iterator[Rdf#Node]]
  = {
    println(s"search 2: starting TRANSACTION for dataset $dataset")
    val transaction =
      dataset.r({
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
    println( s"search_onlyNT(search='$search') \n$queryString \n\tdataset Class ${dataset.getClass().getName}" )
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
