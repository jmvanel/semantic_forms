package deductions.runtime.sparql_cache.algos

import java.net.URL
import scala.util.Try
import scala.util.Success

import org.w3.banana.PointedGraph
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.PointedGraph
import org.w3.banana.PointedGraphs
import org.w3.banana.io._

import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.utils.RDFHelpers
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.utils.RDFHelpers
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.services.SPARQLHelpers

/**
 * arguments:
 *  - RDF data URL (typically a SPARQL request)
 *  - URI of Mobile
 *  - (optional) 2 timestamps defining a time interval (ISO format)
 */
object GeoPathApp extends {
  override val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
} with App
    with ImplementationSettings.RDFCache
    with GeoPath[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  import ops._

  val dataURL = new URL(args(0))
  val graph = rdfLoader.load(dataURL)
  println(s"graph $graph")
  graph.map {
    graph =>
      val (pathLength, timeColumns) =
        if (args.size == 4) {
          val mobile = URI(args(1))
          val begin = args(2)
          val end = args(3)
          (getPathLengthForMobileInterval(mobile, begin, end, graph), s"$begin\t$end")
        } else if (args.size == 2) {
          val mobile = URI(args(1))
          (getPathLengthForMobile(mobile, graph), s"\t")
        } else ("", "")

      if (args.size == 1) {
        println("getPathLengthForAllMobiles")
        println(
          getTriples(getPathLengthForAllMobiles(graph)).mkString("\n"))
      } else {
        val mobile = URI(args(1))
        println(s"""Path Length For Mobile (km)
        <${mobile}>\t$timeColumns\t${pathLength}""")
      }
  }
}

trait GeoPath[Rdf <: RDF, DATASET]
    extends RDFPrefixes[Rdf]
    with SPARQLHelpers[Rdf, DATASET]
    with RDFHelpers[Rdf] {

  implicit val ops: RDFOps[Rdf]
  import ops._
  val rdfLoader: RDFLoader[Rdf, Try]

  val precedingPointProp = geoloc("precedingPoint")
  val mobileProp = geoloc("mobile")

  /** create a graph to populate a database of statistics:
   *  - geoloc:totalDistanceTraveled
   *  */
  def getPathLengthForAllMobiles(graph: Rdf#Graph): Rdf#Graph = {
    // global distances
    val mobiles = getMobileList(graph)
    val paths = for( mobile <- mobiles) yield {
      println(s"mobile ${mobile}");
      val dist = getPathLengthForMobile(mobile, graph)
      Triple(mobile, geoloc("totalDistanceTraveled"), Literal(dist toString(), xsd.float))
    }
    val gr1 = makeGraph(paths)
    // per day distances TODO
    // per half day distances TODO
    gr1
  }

  def getPathLengthForMobile(mobile: Rdf#Node, graph: Rdf#Graph): Float = {
    getPathLength(getPathForMobile(mobile, graph))
  }

  def getPathLengthForMobileInterval(mobile: Rdf#Node, begin: String, end: String, graph: Rdf#Graph): Float = {
    getPathLength(
      filterPointsByTimeInterval(
        getPathForMobile(mobile, graph),
        begin, end))
  }
    
  /** get Path Length in kilometers */
  private def getPathLength(graph: Iterable[PointedGraph[Rdf]]): Float = {
    def pow2(v: Float) = v * v
    println(s"in getPathLength size ${graph.size}")
    val coords = for (
      triplesAboutPoint <- graph;
//      _ = println(s"graph size ${graph.size}");
      long = (triplesAboutPoint / geo("long")).as[Rdf#Literal] match {
        case Success(lit1) => fromLiteral(lit1)._1 . toFloat
        case _ => Float.NaN // 0f
      };     
      lat = (triplesAboutPoint / geo("lat")).as[Rdf#Literal] match {
        case Success(lit1) => fromLiteral(lit1)._1 . toFloat
        case _ => Float.NaN // 0f
      }
    ) yield {
//    	println(s"in getPathLength (long, lat) ${(long, lat)}")
      (long, lat) }
    
    var distanceInDegrees = 0f
    val res = coords.reduceLeft(
        (a, b) => {
          val distanceDelta= Math.sqrt(pow2(b._1 - a._1) + pow2(b._2 - a._2)).toFloat
//          println( s"distanceInDegrees delta=${distanceDelta} a=$a b=$b")
          if( ! distanceDelta.isNaN())
            distanceInDegrees += distanceDelta
          b
        }
    		)
    println( s"distanceInDegrees $distanceInDegrees")
    distanceInDegrees * 6371 * Math.PI / 180 toFloat
  }

  /**
   * get Path For Mobile
   *  @return Iterable of mini RDF graphs with all data attached to each space-time point
   */
  private def getPathForMobile(mobile: Rdf#Node, graph: Rdf#Graph): Iterable[PointedGraph[Rdf]] = {
    val points = getPointsForMobile(mobile, graph)
    println(s"called getPointsForMobile <$mobile> : size ${points.size}")
    for (
      point <- points;
      triplesAboutPoint = find(graph, point, ANY, ANY)
    ) yield {
//          println(s"getPathForMobile: $point")
          PointedGraph(point, makeGraph(triplesAboutPoint.toIterable)) }
  }

  /** get Points (URI's) For Mobile, ordered by using dct:date */
  private def getPointsForMobile(mobile: Rdf#Node, graph: Rdf#Graph): Iterable[Rdf#Node] = {
    val unordered = getPointsForMobileUnordered(mobile, graph)
    def compareByDate(point1: Rdf#Node, point2: Rdf#Node): Boolean = {
      val pgraph1 = PointedGraph(point1, graph)
      val pgraph2 = PointedGraph(point2, graph)
      (pgraph1 / dct("date")).as[Rdf#Literal].toString >
        (pgraph2 / dct("date")).as[Rdf#Literal].toString
    }
    unordered.nodes.toSeq.sortWith(compareByDate)
  }

  private def getPointsForMobileUnordered(mobile: Rdf#Node, graph: Rdf#Graph): PointedGraphs[Rdf] = {
    val pgraph = PointedGraph(mobile, graph)
    //  ?POINT geoloc:mobile <mobile> .
    (pgraph /- mobileProp)
  }

  private def filterPointsByTimeInterval(
    points: Iterable[PointedGraph[Rdf]],
    begin: String, end: String): Iterable[PointedGraph[Rdf]] = {
    for (
      triplesAboutPoint <- points;
      timestamp = (triplesAboutPoint / dct("date")).as[Rdf#Literal] match {
        case Success(lit1) => fromLiteral(lit1)._1
        case _             => ""
      };
      if (timestamp >= begin && timestamp <= end)
    ) yield { triplesAboutPoint }
  }

  private def getMobileList(graph: Rdf#Graph): Iterable[Rdf#Node] = {
    val sparql =
      s"""|${declarePrefix(geoloc)}
                    |SELECT DISTINCT ?mobile
                    |WHERE {
                    | # GRAPH ?GR {
                    |  ?point geoloc:mobile ?mobile .
                    | # }
                    |}
      """.stripMargin

    val res = runSparqlSelectNodes(sparql, Seq("mobile"), graph)
    println(s"getMobileList: res $res")

    for (
      row <- res;
    	_ = println(s"getMobileList:row  $row") ;
      uri <- row.headOption
    ) yield uri
    //    val triplesAboutPoint = find(graph, ANY, rdf.typ, geoloc("Mobile"))
//    Seq(URI("imei:863977030715952"), URI("imei:863977030716091"),
//    		URI("imei:863977030771070"))
  }

  //  private def nextPoint(point: Rdf#Node)(implicit graph: Rdf#Graph) = {
  //    val pgraph = PointedGraph(point, graph)
  //    (pgraph /- precedingPointProp).as[Rdf#Node]
  //  }
}