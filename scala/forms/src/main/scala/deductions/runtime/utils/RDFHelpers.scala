package deductions.runtime.utils

import org.w3.banana.PointedGraph
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFPrefix

/** */
trait RDFHelpers[Rdf <: RDF] extends RDFHelpers0[Rdf] {
  implicit val ops: RDFOps[Rdf]
  val rdfh: RDFHelpers[Rdf]  = this
  import rdfh.{ops=>_, _}
  import ops.{ rdf=>_, _}

  /** recursively iterate on the Rdf#Node through rdf:first and rdf:rest */
  def rdfListToSeq(listOp: Option[Rdf#Node], result: Seq[Rdf#Node] = Seq())
  (implicit graph: Rdf#Graph)
  : Seq[Rdf#Node] = {
    listOp match {
      case None => result
      case Some(list) =>
        list match {
          case rdf.nil => result
          case _ =>
            val first = ops.getObjects(graph, list, rdf.first)
            val rest = ops.getObjects(graph, list, rdf.rest)
            result ++ first ++ rdfListToSeq(rest.headOption, result)
        }
    }
  }

  /** Query for objects in triples, given subject & predicate
   *  NOTE: this function in core Banana does the job:
   *  getObjects(graph: Rdf#Graph, subject: Rdf#Node, predicate: Rdf#URI): Iterable[Rdf#Node]
   *  */
  def objectsQuery(subject: Rdf#Node, predicate: Rdf#URI)
    (implicit graph: Rdf#Graph)
    : Set[Rdf#Node] = {
    val pg = PointedGraph[Rdf](subject, graph)
    val objects = pg / predicate
    objects.map(_.pointer).toSet
  }

  def objectsQueries[T <: Rdf#Node](subjects: Set[T], predicate: Rdf#URI)
    (implicit graph: Rdf#Graph)
    : Set[Rdf#Node] = {
    val values = for (
      subject <- subjects;
      values <- objectsQuery(subject.asInstanceOf[Rdf#URI], predicate)
    ) yield values
    values
  }

//  /** replace all triples
//   *  <subject> <predicate> ?O .
//   *  (if any)
//   *  with a single one:
//   *  <subject> <predicate> <objet> .
//   *  */
//  def replaceObjects(graph: Rdf#Graph, subject: Rdf#Node, predicate: Rdf#URI,
//      objet: Rdf#Node): Unit = {
////	  val mgraph = graph.makeMGraph()
//    val objectsToRemove = getObjects(graph, subject, predicate)
//    for( obj <- objectsToRemove ) {
//    	ops.removeTriple( mgraph, Triple(subject, predicate, obj) )
//    }
//    ops.addTriple( mgraph, Triple(subject, predicate, objet) )
//  }

  def replaceSameLanguageTriple(triple: Rdf#Triple,
                                mgraph: Rdf#MGraph)
    (implicit graph: Rdf#Graph)
    : Int = {
    val language = getLang(triple.objectt)
    val count =
      if (language != NL) {
        val objects = objectsQuery(triple.subject, triple.predicate)
        val objectsMatchingLanguage = objects.filter {
          n => getLang(n) == language
        }
        objectsMatchingLanguage map {
          lit =>
            {
              removeTriple(mgraph, Triple(triple.subject, triple.predicate, lit))
              println(triple)
            }
        }
        addTriple(mgraph, triple)
        objectsMatchingLanguage.size
      } else 0
    count
  }
}


trait RDFHelpers0[Rdf <: RDF] {
  implicit val ops: RDFOps[Rdf]
  import ops._
  val rdf = RDFPrefix[Rdf](ops)

  /** from given Set of Rdf#Node , extract rdf#URI */
  def nodeSeqToURISeq(s: Iterable[Rdf#Node]): Seq[Rdf#URI] = {
    val r = s.collect {
      case uri if (isURI(uri)) => ops.makeUri(uri.toString)
    }
    val seq = r.to
    seq
  }

  /** from given Set of Rdf#Node , extract rdf#URI */
  def nodeSeqToURISet(s: Iterable[Rdf#Node]): Set[Rdf#URI] = {
    nodeSeqToURISeq(s).toSet
  }

  /**
   * from given Set of Rdf#Node , extract rdf#URI
   *  TODO : check that it's the same as nodeSeqToURISet
   */
  private def extractURIs(nodes: Set[Rdf#Node]): Set[Rdf#URI] = {
    nodes.map {
      node =>
        ops.foldNode(node)(
          identity, identity, x => None)
    }
      .filter(_ != None)
      .map { node => node.asInstanceOf[Rdf#URI] }
  }

  def isURI(node: Rdf#Node) = ops.foldNode(node)(identity, x => None, x => None) != None

  def getStringOrElse(n: Rdf#Node, default: String): String = {
    ops.foldNode(n)(_ => default, _ => default, l => {
      val v = ops.fromLiteral(l)
      v._1
    })
  }

  def getLiteralNodeOrElse(n: Rdf#Node, default: String): Rdf#Node = {
    val d = ops.Literal(default)
    ops.foldNode(n)(_ => d, _ => d, l => l)
  }

  def printGraph(graph: Rdf#Graph) {
    val iterable = ops.getTriples(graph)
    for (t <- iterable) {
      println(t)
      val (subj, pred, obj) = ops.fromTriple(t)
    }
  }

  val NL = makeLang("No_language")
  def getLang(node: Rdf#Node): Rdf#Lang = {
    foldNode(node)(
      l => NL,
      l => NL,
      l => fromLiteral(l)._3 match {
        case Some(lang) => lang
        case _          => NL
      })
  }
}