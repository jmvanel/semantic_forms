package deductions.runtime.sparql_cache.algos

import org.w3.banana.OWLPrefix
import org.w3.banana.PointedGraph
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFSPrefix
import org.w3.banana.PointedGraph

import deductions.runtime.html.HTML5TypesTrait
import java.io.PrintStream
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.services.Configuration
import deductions.runtime.services.SPARQLHelpers



trait DuplicatesDetectionBase[Rdf <: RDF, DATASET]
extends HTML5TypesTrait[Rdf]
with RDFPrefixes[Rdf]
with SPARQLHelpers[Rdf, DATASET]
{
    this: Configuration =>

  val FILE_OUTPUT = true
  val printStream: PrintStream = System.out
  val ontologyPrefix: String
  val detailedLog = false

  implicit val ops: RDFOps[Rdf]
  import ops._
  private lazy val owl = OWLPrefix[Rdf]

  /** @return the Instances URI's */
  def findInstances(graph: Rdf#Graph, typeURI: Rdf#URI=owl.DatatypeProperty): List[Rdf#Node] = {
    outputErr(s"Triple count ${graph.size}")
    val datatypeProperties = find(graph, ANY, rdf.typ, typeURI)
    val datatypePropertiesURI = datatypeProperties.map { triple => triple.subject }.toList
    outputErr(s"Instances count <$typeURI> ${datatypePropertiesURI.size}\n")
    datatypePropertiesURI
  }

  /** @return URI of meta-Class To Report:
   *  owl:ObjectProperty 
   *  owl:Class
   *  owl:DatatypeProperty
   *  (second command line argument if any) */
  def owlMetaClassToReport(args: Array[String]): Rdf#URI =
    if (args.size > 1)
      args(1) match {
        case "owl:ObjectProperty" => owl.ObjectProperty
        case "owl:Class"          => owl.Class
        case "owl:DatatypeProperty" => owl.DatatypeProperty
        case t => URI(t)
//          outputErr(s"case not implemented: '${t}'")
//          System.exit(-1)
//          owl.DatatypeProperty
      }
    else
      owl.DatatypeProperty

  case class Duplicate(d1: Rdf#Node, d2: Rdf#Node) {
    /** cf http://tools.ietf.org/html/rfc4180 */
    def toString(graph: Rdf#Graph): String = {
      def toString(n: Rdf#Node) = {
        abbreviateURI(n) +
          "; " + rdfsLabel(n, graph)
      };

      val r1 = rdfsRangeToString(d1, graph)
      val r2 = rdfsRangeToString(d2, graph)
      if (r1 != r2)
        toString(d1) + "; rdfs:range " + r1 +
          "; " +
          toString(d2) + "; rdfs:range " + r2
      else
        toString(d1) + ";" + ( if(r1 != "text" ) r1 else "" ) +
          "; " +
          toString(d2) + ";"
    }
  }
  case class DuplicationAnalysis( duplicates: List[Duplicate] )
  
  def abbreviateURI(n: Rdf#Node) = n.toString().replace(ontologyPrefix, ":")
  
  def nodesAreSimilar(n1: Rdf#Node, n2: Rdf#Node, graph: Rdf#Graph): Boolean = {
	  haveSimilarLabels(n1, n2, graph) &&
	  haveSameRanges(n1, n2, graph)
  }

  def haveSameRanges(n1: Rdf#Node, n2: Rdf#Node, graph: Rdf#Graph): Boolean = {
    val ranges1 = rdfsRange(n1, graph)
    val ranges2 = rdfsRange(n2, graph)
    //    println(s"ranges1 $ranges1 ranges2 $ranges2")
    val rangesOverlap = !(ranges1.toSet.intersect(ranges2.toSet).isEmpty)
    rangesOverlap
  }
  
  def haveSimilarLabels( n1: Rdf#Node, n2: Rdf#Node, graph: Rdf#Graph): Boolean = {
        val label1 = rdfsLabel(n1, graph)
        val label2 = rdfsLabel(n2, graph)
        stringsAreSimilar(label1, label2)
  }

  def stringsAreSimilar(s1: String, s2: String): Boolean = {
    if( s1 == "" || s2 == "" ) return false

    val words1 = s1.split("""\s+""").toSet
    val words2 = s2.split("""\s+""").toSet
    val intersection = words1 intersect (words2)
    val averageSize = ( words1.size +  words2.size ) * 0.5
    val output = intersection.size > averageSize * 0.5
    if( output ) // intersection.size > 0 )
      log(s"""\tintersection.size >0  $intersection - "$s1" "$s2" """)
    output
  }


  // ==== TODO move these reusable functions ====

  /** print rdfs:label if any Or Else abbreviated Turtle */
  def rdfsLabel( no: Option[Rdf#Node], graph: Rdf#Graph): String =
    no match {
      case Some(n) => rdfsLabel( n, graph)
      case None => ""
    }

  protected def rdfsLabel( n: Rdf#Node, graph: Rdf#Graph): String = printPropertyValue( n, graph)

  protected def printPropertyValue( n: Rdf#Node, graph: Rdf#Graph, prop: Rdf#URI = rdfs.label): String =
		  (PointedGraph( n , graph) / prop ).as[String].getOrElse( abbreviateTurtle(n) )
  private def printObjectPropertyValue( n: Rdf#Node, graph: Rdf#Graph, prop: Rdf#URI = rdfs.label): String =
		  abbreviateTurtle( (PointedGraph( n , graph) / prop ).as[Rdf#URI].getOrElse( (n) ))
  protected def printPropertyValueNoDefault( n: Rdf#Node, graph: Rdf#Graph, prop: Rdf#URI = rdfs.label): String = {
    if ( n.toString() .endsWith("CFA"))
      println()
		  val literal = (PointedGraph( n , graph) / prop ).as[String].getOrElse( "" )
		  if(literal != "" && ! literal.contains(":"))
		    literal
		  else
		    printObjectPropertyValue( n, graph, prop)
  }
	def rdfsDomain(n: Rdf#Node, graph: Rdf#Graph) =
		  (PointedGraph( n , graph) / rdfs.domain) .as[Rdf#Node].getOrElse(URI(""))

	def rdfsSuperClasses(n: Rdf#Node, graph: Rdf#Graph): List[Rdf#Node]= {
	  find(graph, n, rdfs.subClassOf, ANY ) . map { tr => tr.objectt } . toList
  }
		
  def rdfsSubClasses(n: Rdf#Node, graph: Rdf#Graph): List[Rdf#Node]= {
	  find(graph, ANY, rdfs.subClassOf, n ) . map { tr => tr.subject } . toList
  }
  
  /** @return list of rdfs:range's */
  def rdfsRange(p: Rdf#Node, graph: Rdf#Graph) =
    find(graph, p, rdfs.range, ANY).
      map { triple => triple.objectt }.toList

  def rdfsRangeFromClass(c: Rdf#Node, graph: Rdf#Graph) = {
    val props = find(graph, ANY, rdfs.domain, c)
    val v = props . map { trip => val prop = trip.subject; rdfsRange( prop, graph) }
    v.flatten
  }

  def rdfsPropertiesAndRangesFromClass(classe: Rdf#Node, graph: Rdf#Graph): String = {
    val v = rdfsPropertiesAndRangesFromClassList(classe, graph)
    v. mkString(", ")
  }

  def rdfsPropertiesAndRangesFromClassList(classe: Rdf#Node, graph: Rdf#Graph): List[String] = {
     def rdfsPropertyAndRange(prop: Rdf#Node) = {
       rdfsLabel(prop, graph) + ": " +
      rdfsLabel(rdfsRange( prop, graph).headOption, graph)
     }
    val props = find(graph, ANY, rdfs.domain, classe)
    val propsStrings = props . map { triple =>
      val prop = triple.subject
      rdfsPropertyAndRange(prop)
    } . toList

    val q = queryString.replace("<CLASS>", s"<${classe}>")
    val res: List[Seq[Rdf#Node]] = runSparqlSelectNodes( q, List("PROP"), graph)
    val propsFromUnionOf = res . map {
      list => list.head
    }
    val propsAndRangeFromUnionOf = propsFromUnionOf . map {
         prop => rdfsPropertyAndRange(prop)
    }
    if( classe.toString().endsWith("Concours/age/#class"))
      println
    propsStrings ++ propsAndRangeFromUnionOf
  }

  val queryString = """
    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
    PREFIX owl: <http://www.w3.org/2002/07/owl#>

    SELECT *
    WHERE {
      ?PROP rdfs:domain / owl:unionOf / rdf:rest* / rdf:first <CLASS>
   }
  """

  def makeDigestFromClass(classe: Rdf#Node, graph: Rdf#Graph): String = {
    val props = find(graph, ANY, rdfs.domain, classe) . toList
    val urisForRanges = props.map { triple =>
      val prop = triple.subject
      val ranges = rdfsRange(prop, graph)
      val rangesAbbreviatedURIs = ranges.map { range =>
        if( range == xsd.string )
//          "\"" + rdfsLabel(prop, graph) + "\""
          abbreviateURI(prop).split("/").last
        else
        abbreviateTurtle(URI(abbreviateURI(range)))
      }
      println(s"""makeDigestFromClass: class ${abbreviateURI(classe)} prop ${abbreviateURI(prop)} ranges_or_URI $rangesAbbreviatedURIs
          " ${rdfsLabel(prop, graph)}" """)
      rangesAbbreviatedURIs.mkString(", ")
    }
    val urisForRangesAndProps = urisForRanges.toList
    urisForRangesAndProps.sortWith(
      (n1, n2) => (n1) < (n2)).
      mkString("; ")
  }

  def rdfsRangeToString(n: Rdf#Node, graph: Rdf#Graph): String = {
    rdfsRange(n, graph) match {
      case range :: rest =>
        xsdNode2html5TnputType(range)
      case _ => n.toString()
    }
  }
      
  def log(mess: String) = if(detailedLog) println(mess)
  def output(mess: String) = if(FILE_OUTPUT)
    printStream.println(mess)
  else
    println(mess)
  def outputErr(mess: String) = Console.err.println(mess)
}
