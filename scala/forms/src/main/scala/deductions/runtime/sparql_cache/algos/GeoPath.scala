package deductions.runtime.sparql_cache.algos

import org.w3.banana.PointedGraph
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.PointedGraph
import org.w3.banana.PointedGraphs

import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.utils.RDFHelpers
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.utils.RDFHelpers

trait GeoPath[Rdf <: RDF]
    extends RDFPrefixes[Rdf]
//    with SPARQLHelpers[Rdf, DATASET]
    with RDFHelpers[Rdf] {

  implicit val ops: RDFOps[Rdf]
  import ops._

  val precedingPointProp = geoloc("precedingPoint")
  val mobileProp = geoloc("mobile")

  def getPathLengthForMobile(mobile: Rdf#Node, graph: Rdf#Graph) : Float = {
    getPathLength( getPathForMobile(mobile, graph) )
  }

  def getPathLength(graph: Iterable[PointedGraph[Rdf]] ): Float = {
		def pow2(v: Float)= v*v
    val coords = for (
      triplesAboutPoint <- graph ;
      point = triplesAboutPoint.pointer ;
      graph = triplesAboutPoint.graph ;
      long = (triplesAboutPoint / geo("long")) .as[Rdf#Literal] .toString .toFloat ;
      lat = (triplesAboutPoint / geo("lat")) .as[Rdf#Literal] .toString .toFloat
    ) yield {(long, lat)}
    val res = coords.reduceLeft((a,b) => (Math.sqrt(pow2(b._1 - a._1) + pow2(b._2 - a._2)).toFloat, 0) )
    val distanceInDegrees = res._1
    distanceInDegrees * 6371 * Math.PI / 180 toFloat
  }

  /**
   * get Path For Mobile
   *  @return Iterable of mini RDF graphs with all data attached to each space-time point
   */
  def getPathForMobile(mobile: Rdf#Node, graph: Rdf#Graph): Iterable[PointedGraph[Rdf]] = {
    val points = getPointsForMobile(mobile, graph)
    for (
      point <- points;
      triplesAboutPoint = find(graph, point, ANY, ANY)
    ) yield { PointedGraph( point, makeGraph(triplesAboutPoint.toIterable) )}
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

  //  private def nextPoint(point: Rdf#Node)(implicit graph: Rdf#Graph) = {
  //    val pgraph = PointedGraph(point, graph)
  //    (pgraph /- precedingPointProp).as[Rdf#Node]
  //  }
}