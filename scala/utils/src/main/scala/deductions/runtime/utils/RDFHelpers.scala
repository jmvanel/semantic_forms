package deductions.runtime.utils

import org.w3.banana.binder.PGBinder
import org.w3.banana.{OWLPrefix, PointedGraph, Prefix, RDF, RDFOps, RDFPrefix}

import scala.util.{Failure, Success}

//import deductions.runtime.abstract_syntax.UnfilledFormFactory

import scala.util.control.NonFatal

import scalaz._
import Scalaz._

/** */
trait RDFHelpers[Rdf <: RDF] extends RDFHelpers0[Rdf] {

  implicit val ops: RDFOps[Rdf]
//  val rdfh: RDFHelpers[Rdf] = this
//  import rdfh.{ ops => _, _ }
  import ops.{rdf => _, _}

  /** recursively iterate on the Rdf#Node through rdf:first and rdf:rest */
  def rdfListToSeq(listOp: Option[Rdf#Node], result: Seq[Rdf#Node] = Seq())(implicit graph: Rdf#Graph): Seq[Rdf#Node] = {
    listOp match {
      case None => result
      case Some(list) =>
        list match {
          case rdf.nil => result
          case _ =>
            val first = getObjects(graph, list, rdf.first)
            val rest = getObjects(graph, list, rdf.rest)
            result ++ first ++ rdfListToSeq(rest.headOption, result)
        }
    }
  }

  /**
   * Query for objects in triples, given subject & predicate
   *  NOTE: this function in core Banana does the job:
   *  getObjects(graph: Rdf#Graph, subject: Rdf#Node, predicate: Rdf#URI): Iterable[Rdf#Node]
   *  TODO : predicate should be a Node
   */
  def objectsQuery(subject: Rdf#Node, predicate: Rdf#URI)
  (implicit graph: Rdf#Graph): Set[Rdf#Node] = {
    val pg = PointedGraph[Rdf](subject, graph)
    val objects = pg / predicate
    objects.map(_.pointer).toSet
  }

  def objectsQueries[T <: Rdf#Node](subjects: Set[T], predicate: Rdf#URI)(implicit graph: Rdf#Graph): Set[Rdf#Node] = {
    val values = for (
      subject <- subjects;
      values <- objectsQuery(subject.asInstanceOf[Rdf#URI], predicate)
    ) yield values
    values
  }

  def getClasses(subject: Rdf#Node)(implicit graph: Rdf#Graph): List[Rdf#Node] =
    if (subject == nullURI)
      List()
    else
      getObjects(graph, subject: Rdf#Node, rdf("type")).toList

  def getClassOrNullURI(subject: Rdf#Node)(implicit graph: Rdf#Graph): Rdf#Node =
    getClasses(subject).headOption.getOrElse(nullURI)

  /** replace Same Language triple(s):
   *  given triple ?S ?P ?O ,
   *  remove triple(s)  ?S ?P ?O1 whose language matches input triple,
   *  and finally add given triple */
  def replaceSameLanguageTriple(triple: Rdf#Triple,
                                mgraph: Rdf#MGraph)(implicit graph: Rdf#Graph): Int = {
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

  /**
   * get first ?OBJ such that:
   *   subject predicate ?OBJ	,
   *   or returns default URI
   */
  def getHeadOrElse(subject: Rdf#Node, predicate: Rdf#URI,
    default: Rdf#URI = nullURI)
    (implicit graph: Rdf#Graph)
  : Rdf#URI = {
    objectsQuery(subject, predicate) match {
      case ll if ll.isEmpty => default
      case ll if (isURI(ll.head)) => ll.head.asInstanceOf[Rdf#URI]
//      case _ => default
    }
  }

  /**
   * get first ?OBJ such that:
   *   subject predicate ?OBJ	,
   *   or returns default string
   */
  def getStringHeadOrElse(subject: Rdf#Node, predicate: Rdf#URI,
    default: String="")
    (implicit graph: Rdf#Graph)
  : String = {
	  getStringHead(objectsQuery(subject, predicate).toList, default)
  }

  def getStringHead(list: List[Rdf#Node], default: String = ""): String = {
    list match {
      case ll if ll.isEmpty => default
      case ll               => literalNodeToString(ll.head)
    }
  }
}


trait RDFHelpers0[Rdf <: RDF]
extends URIManagement {

  implicit val ops: RDFOps[Rdf]
  import ops._
  
  lazy val nullURI = URI("")
  lazy val rdf = RDFPrefix[Rdf](ops)

  /** from given Set of Rdf#Node , extract rdf#URI */
  def nodeSeqToURISeq(s: Iterable[Rdf#Node]): Seq[Rdf#URI] = {
    val r = s.collect {
      case uri if (isURI(uri)) => makeUri(uri.toString)
    }
    val seq = r.to
    seq
  }

  /** from given Rdf#Node , make a rdf#URI, or null URI */
  def nodeToURI(n: Rdf#Node): Rdf#URI = {
    foldNode(n)(u => u,
      b => nullURI,
      l => nullURI)
  }

//  /** from given Set of Rdf#Node , extract rdf#URI */
//  def nodeSeqToURISet(s: Iterable[Rdf#Node]): Set[Rdf#URI] = {
//    nodeSeqToURISeq(s).toSet
//  }

  /**
   * from given Set of Rdf#Node , extract rdf#URI
   *  TODO : check that it's the same as nodeSeqToURISet
   */
  private def extractURIs(nodes: Set[Rdf#Node]): Set[Rdf#URI] = {
    nodes.map {
      node =>
        foldNode(node)(
          identity, identity, x => None)
    }
      .filter(_ != None)
      .map { node => node.asInstanceOf[Rdf#URI] }
  }

  def isURI(node: Rdf#Node) = foldNode(node)(identity, x => None, x => None) != None
  def isBN(node: Rdf#Node) = foldNode(node)(x => None, identity, x => None) != None
  def toBN(node: Rdf#Node): Rdf#BNode = foldNode(node)(x => BNode(""), identity, x => BNode(""))
  def isCorrectURI(node: Rdf#Node): Boolean =
    foldNode(node)(uri =>
      isCorrectURI(fromUri(uri)), x => None, x => None) != None
  def isAbsoluteURI(node: Rdf#Node): Boolean =
    foldNode(node)(uri =>
      isAbsoluteURI(fromUri(uri)), x => None, x => None) != None
      
  /** use case : when we know that the node is a literal */
  def literalNodeToString(node: Rdf#Node): String =
    foldNode(node)(
      x => "", x => "",
      literal => fromLiteral(literal)._1)
  /** use case : when we know that the node is an URI */
  def uriNodeToURI(node: Rdf#Node): Rdf#URI =
    foldNode(node)(
      uri => uri, x => URI(""),
      literal => URI("") )
  /** avoids the "" around the literal */
  def nodeToString(node: Rdf#Node): String =
    foldNode(node)(
      uri => fromUri(uri),
      bn => fromBNode(bn),
      literal => fromLiteral(literal)._1)
 
  def compareTriples( tr1: Rdf#Triple,tr2: Rdf#Triple ): Boolean =
    tr1.subject == tr2.subject &&
    tr1.objectt == tr2.objectt &&
    tr1.predicate == tr2.predicate

  /** url not hosted here, and
   *  must start with http: ftp: file: https: */
  def isDownloadableURI(uri: Rdf#URI) = {
    val u = fromUri(uri)
    isDownloadableURL(u) &&
      // not Downloadable in this case, because it's hosted here ! 
      !u.startsWith(instanceURIPrefix)

    // TODO ?
    // u.startsWith("_:")
  }
  
  def getStringOrElse(n: Rdf#Node, default: String =""): String = {
    foldNode(n)(_ => default, _ => default, l => {
      val v = fromLiteral(l)
      v._1
    })
  }

  def getLiteralNodeOrElse(n: Rdf#Node, default: String): Rdf#Node = {
    val d = Literal(default)
    foldNode(n)(_ => d, _ => d, l => l)
  }

  def firstNodeOrElseNullURI(set: Iterable[Rdf#Node]): Rdf#Node = set.headOption.getOrElse(nullURI)

  /**
   * compute terminal Part of URI, eg
   *  Person from http://xmlns.com/foaf/0.1/Person
   *  Project from http://usefulinc.com/ns/doap#Project
   *  NOTE: code related for getting the ontology prefix
   */
  def terminalPart(n: Rdf#Node): String = {
    foldNode(n)(
      uri => getFragment(uri) match {
        case None       => last_segment(uri) // lastSegment(uri)
        case Some(frag) => frag
      },
      bNode => "",
      literal => "")
  }
  
  /** NOTE: currently lastSegment() in Banana can return null :( */
  def last_segment(node: Rdf#Node): String =
    try {
      foldNode(node)(
        uri => {
          val ls = ops.lastSegment(uri)
          val uriString = fromUri(uri)
//          println(s"last_segment 1 $uriString '$ls'")
          ls match {
            case "" => uriString
            case _  =>
//              val uri2 = uriString.substring(0, (uriString.length - ls.length) )
//              println(s"last_segment 2 $uriString '$ls'")
//              last_segment( URI( uri2 )) +
//              "/" +
              ls + "#" + getFragment(uri).getOrElse("")
          }
        },
        bn => bn.toString(),
        x => x.toString())
    } catch {
      case NonFatal(e) => node.toString()
    }
    
  def printGraph(graph: Rdf#Graph) {
    val iterable = getTriples(graph)
    for (t <- iterable) {
      println(t)
      val (subj, pred, obj) = fromTriple(t)
    }
  }

  lazy val NL = makeLang("No_language")
  def getLang(node: Rdf#Node): Rdf#Lang = {
    foldNode(node)(
      l => NL,
      l => NL,
      l => fromLiteral(l)._3 match {
        case Some(lang) => lang
        case _          => NL
      })
  }

  /** declare Prefix in SPARQL */
  def declarePrefix(pref: Prefix[Rdf]) = {
    s"PREFIX ${pref.prefixName}: <${pref.prefixIri}> "
  }
  
  import scala.language.postfixOps
  
  /** @return RDF list of URI's <l1> <l2> ,,, such that
   * <classe> owl:unionOf ( <l1> <l2> ) . */
  def processUnionOf(graph: Rdf#Graph, classe: Rdf#Node): Seq[Rdf#Node] = {
    val owl = OWLPrefix[Rdf]
    val rdfLists = getObjects(graph, classe, owl.unionOf)
    val binder = PGBinder[Rdf, List[Rdf#Node]]
    if (!(rdfLists isEmpty)) {
      val rdfList = rdfLists.head
      val classesTry = binder.fromPG(PointedGraph(rdfList, graph))
      classesTry match {
        case Success(classes) => classes.toSeq
        case Failure(e)       => Seq()
      }
    } else Seq()
  }

  /**
   * from an Rdf#Node, print the turtle term;
   * betehess 15:22
   * @ jmvanel nothing giving you that out-of-the-box right now
   * I'd write a new typeclass to handle that
   * it's super easy to do
   */
  def makeTurtleTerm(node: Rdf#Node) =
    foldNode(node)(
      uri => s"<${fromUri(uri)}>",
      bn => s"<_:${fromBNode(bn)}>",
      // TODO: datatype
      lit => {
        val rawString = fromLiteral(lit)._1
        val datatype = fromLiteral(lit)._2
        val langOption = fromLiteral(lit)._3
        val suffix = langOption match {
          case Some(lang) => "@" + fromLang(lang)
          case None => if( datatype != xsd.string ) "^^<" + fromUri(datatype) + ">" else ""
        }
        val wrapping = if (rawString.contains("\"") ||
          rawString.contains("\n"))
          "\"" * 3
        else
          "\""
        /* FIX ERROR riot [line: 2, col: 13] Illegal escape sequence value: (0x0D) ,
         * occuring in a <pre> block with \ followed by carriage return */
        val turtleString = rawString.replaceAll("""\\\r""", """\\\\\u000d""")
        wrapping + turtleString + wrapping + suffix
      })
}
