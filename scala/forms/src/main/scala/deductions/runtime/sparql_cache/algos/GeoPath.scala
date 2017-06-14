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

/**
 * arguments:
 *  - URI of Mobile
 *  - RDF data URL (typically a SPARQL request)
 *  - (optional) 2 timestamps defining a time interval (ISO format)
 */
object GeoPathApp extends {
  override val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
} with App
    with ImplementationSettings.RDFCache
    with GeoPath[ImplementationSettings.Rdf] {
  import ops._

  val dataURL = new URL(args(1))
  val graph = rdfLoader.load(dataURL)
  graph.map {
    graph =>
      val mobile = URI(args(0))
      val pathLength =
        if (args.size == 4) {
          val begin = args(2)
          val end = args(3)
          getPathLengthForMobileInterval(mobile, begin, end, graph)
        } else
          getPathLengthForMobile(mobile, graph)
      println(s"Path Length For Mobile <${mobile}> ${pathLength} km")
  }
}

trait GeoPath[Rdf <: RDF]
    extends RDFPrefixes[Rdf]
    //    with SPARQLHelpers[Rdf, DATASET]
    with RDFHelpers[Rdf] {

  implicit val ops: RDFOps[Rdf]
  import ops._
  val rdfLoader: RDFLoader[Rdf, Try]

  val precedingPointProp = geoloc("precedingPoint")
  val mobileProp = geoloc("mobile")

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
  def getPathLength(graph: Iterable[PointedGraph[Rdf]]): Float = {
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
  def getPathForMobile(mobile: Rdf#Node, graph: Rdf#Graph): Iterable[PointedGraph[Rdf]] = {
    val points = getPointsForMobile(mobile, graph)
    println(s"called getPointsForMobile size ${points.size}")
    for (
      point <- points;
      triplesAboutPoint = find(graph, point, ANY, ANY)
    ) yield {
//          println(s"getPathForMobile: $point")
          PointedGraph(point, makeGraph(triplesAboutPoint.toIterable)) }
  }

  /** get Points (URI's) For Mobile, ordered by using dct:date */
  def getPointsForMobile(mobile: Rdf#Node, graph: Rdf#Graph): Iterable[Rdf#Node] = {
    val unordered = getPointsForMobileUnordered(mobile, graph)
    def compareByDate(point1: Rdf#Node, point2: Rdf#Node): Boolean = {
      val pgraph1 = PointedGraph(point1, graph)
      val pgraph2 = PointedGraph(point2, graph)
      (pgraph1 / dct("date")).as[Rdf#Literal].toString >
        (pgraph2 / dct("date")).as[Rdf#Literal].toString
    }
    unordered.nodes.toSeq.sortWith(compareByDate)
  }

  def getPointsForMobileUnordered(mobile: Rdf#Node, graph: Rdf#Graph): PointedGraphs[Rdf] = {
    val pgraph = PointedGraph(mobile, graph)
    //  ?POINT geoloc:mobile <mobile> .
    (pgraph /- mobileProp)
  }

  def filterPointsByTimeInterval(
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

  //  private def nextPoint(point: Rdf#Node)(implicit graph: Rdf#Graph) = {
  //    val pgraph = PointedGraph(point, graph)
  //    (pgraph /- precedingPointProp).as[Rdf#Node]
  //  }
}