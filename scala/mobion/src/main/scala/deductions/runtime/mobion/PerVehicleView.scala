package deductions.runtime.mobion

import deductions.runtime.abstract_syntax.{ FormSyntaxFactory, FormSyntaxFromSPARQL }
import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.html.TableView
import deductions.runtime.sparql_cache.algos.GeoPath
import deductions.runtime.utils.RDFPrefixesInterface
import deductions.runtime.core.HTTPrequest
import deductions.runtime.core.SemanticController

import org.w3.banana.RDF

import scala.xml.NodeSeq
import deductions.runtime.abstract_syntax.uri.URIForDisplayFactory
import deductions.runtime.views.ResultsDisplay
import java.util.Calendar
import org.w3.banana.PointedGraph
import scala.util.Success
import scala.util.Failure
import deductions.runtime.services.ParameterizedSPARQL

/** Per Vehicle View */
trait PerVehicleView[Rdf <: RDF, DATASET]
    extends GeoPath[Rdf, DATASET]
    with SemanticController
    with FormSyntaxFactory[Rdf, DATASET]
    with FormSyntaxFromSPARQL[Rdf, DATASET]
    with TableView[Rdf#Node, Rdf#URI]
    with RDFPrefixesInterface
//  with URIForDisplayFactory[Rdf, DATASET]
    with ResultsDisplay[Rdf, DATASET]
   	with ParameterizedSPARQL[Rdf, DATASET]
    {

  import ops._

  override val featureURI: String = fromUri(geoloc("stats2"))

  def result(request: HTTPrequest): NodeSeq = {

    println(s"result 1")
    val vehicles = enumerateVehicles()
    println(s"result 2")

    // 2) get details for each vehicle

    def vehicleSPARQL(mobile: String) = s"""
      |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
      |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
      |PREFIX geoloc: <http://deductions.github.io/geoloc.owl.ttl#>
      |PREFIX vehman: <http://deductions.github.io/vehicule-management.owl.ttl#>
      |PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
      |CONSTRUCT {
      |} WHERE {
      | { GRAPH ?GR {
      |    $mobile a geoloc:Mobile .
      |   }
      | } OPTIONAL {
      |   GRAPH ?GR1 {
      |       $mobile ?P ?O .
      |       $mobile vehman:vehicle ?VEHICULE.
      |       ?VEHICULE vehman:internalNumber ?NUM.
      |   }
      | }
      |}
      """.stripMargin

    showVehicles(vehicles, request) ++
    // 3) layouts details for each vehicle
    showVehiclesDetails(vehicles, request)

  }

  /** 1) enumerate vehicules (actually mobiles) */
  private def enumerateVehicles(): List[Rdf#Node] = {
    val vehiculesSPARQL = """
      |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
      |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
      |PREFIX geoloc: <http://deductions.github.io/geoloc.owl.ttl#>
      |PREFIX vehman: <http://deductions.github.io/vehicule-management.owl.ttl#>
      |PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
      |SELECT *
      |WHERE {
      | GRAPH ?GR {
      |   { ?MOB a geoloc:Mobile . }
      |   UNION
      |   { ?MOB a vehman:SIMCard . }
      | }
      |}
      """.stripMargin
    val mobilesListSeq = sparqlSelectQueryVariables(vehiculesSPARQL, Seq("MOB"))
    val mobiles = mobilesListSeq.map {
      e =>
        val uri = e.headOption.getOrElse(URI(""))
        uri
    }
    mobiles
  }

  private def showVehicles(vehicles: List[Rdf#Node], request: HTTPrequest): NodeSeq = {
	println(s"result 2.1 vehicles $vehicles")

    val vehiclesHTML = wrapInTransaction {
//	  	println(s"result 2.2 vehicles $vehicles")
      val r = vehicles.map { vehicle =>
//      println(s"result 3 vehicle $vehicle")
        <p>
          {
            // hyperlink  to SF specific wiew
            makeHyperlinkForURI( vehicle, allNamedGraph, request=request )
//            val dis = makeURIForDisplay(vehicle)(allNamedGraph, request.getLanguage())
//            dis.label
          }
        </p>
      }
	  	println(s"result 2.3 vehicles $vehicles")
	  	r
    } match {
      case Success(res) =>
        println(s"result 2.4 res $res " )
        res
      case Failure(f)   =>
        println(s"result 2.4 Error in showVehicles: { $f }")
        <p>Error in showVehicles: { f } </p>
    }
    
    <div class="entete_vehicules">
    <h3>Véhicules</h3>
    { vehiclesHTML }
    </div>
  }

  /** display 2 templates of 3 Days */
  private def showVehiclesDetails(vehicles: List[Rdf#Node], request: HTTPrequest): NodeSeq = {
		val startDate = Calendar.getInstance
		// TODO get past reports
		// for tests:
//		import Calendar._
//		startDate.set( MONTH, JUNE)
//		startDate.set( DAY_OF_MONTH, 8)

    val vehiclesHTML =
      for (
        vehicle <- vehicles;
        pathForMobileTry = wrapInReadTransaction {
          val pathForMobile = getPathForMobile(vehicle, allNamedGraph)
          println(s"==== pathForMobile $vehicle size ${pathForMobile.size}")
          pathForMobile
        } ;
        pathForMobile = pathForMobileTry.getOrElse(Seq() ) ;
        mondays = generateMondays(startDate);
        monday <- mondays
      ) yield {
        val followingDate = addDays(monday, 3)

        weekHeader( vehicle,
            formatReadable(makeBeginOfDay(monday)),
            formatReadable(addDays(makeBeginOfDay(monday), 6))
        ) ++
        template3Days(vehicle, 1, monday, pathForMobile) ++
          template3Days(vehicle, 4, followingDate, pathForMobile)
      }

    vehiclesHTML.flatten
  }

  private def weekHeader(vehicle: Rdf#Node, begin: String, end: String) =
    <div class="entete_semaine">
      <h2>Véhicule {vehicle}</h2>
      <h3>Détail d'activité de la semaine du {begin} au {end}</h3>
      <p>
        CRUIS RENT vous donne le détail de votre activité de livraison par service
           lors de la semaine d'évaluation
      </p>
    </div>

  private def template3Days(
    vehicle: Rdf#Node,
    day: Int = 1, date: Calendar,
    pathForMobile: Iterable[PointedGraph[Rdf]]) = {

    // make VehicleStatistics for each half day
    def doDistancesForADay(date: Calendar): Seq[VehicleStatistics] = {
      val halfDays = generateHalfDays(date)
      val transaction = wrapInReadTransaction {
        for (halfDay <- halfDays) yield {
          val begin = halfDay._1
          val end = halfDay._2
          val totalDistance = getPathLengthForMobileInterval2(vehicle, begin, end,
            pathForMobile)
          println( s"doDistancesForADay: $vehicle: totalDistance=$totalDistance" )
          VehicleStatistics(
            begin,
            end,
            0, // averageDistance
            totalDistance)
        }
      }
      transaction match {
        case Success(s) => s
        case Failure(f) =>
          System.err.println(s"doDistancesForADay: Failure: $f")
          Seq()
      }
    }
    val statisticsDay1 = doDistancesForADay(date)
    val statisticsDay2 = doDistancesForADay(addDays(date, 1))
    val statisticsDay3 = doDistancesForADay(addDays(date, 2))

    val MORNING = 0
    val EVENING = 1
    
    <div class="tab_global_bas">
      <!-- TABLEAU RESULTATS DÉTAILLÉS-->
      <table cellpadding="10" width="90%">
        <tr>
          <th colspan="2"></th>
          <th colspan="4">Jour { day }     - { formatReadable(date) }</th>
          <th colspan="4">Jour { day + 1 } - { formatReadable(addDays(date, 1)) }</th>
          <th colspan="4">Jour { day + 2 } - { formatReadable(addDays(date, 2)) }</th>
        </tr>
        <tr>
          <td colspan="2">Immat.</td>
          <td colspan="2">Après-midi</td>
          <td colspan="2">Soirée</td>
          <td colspan="2">Après-midi</td>
          <td colspan="2">Soirée</td>
          <td colspan="2">Après-midi</td>
          <td colspan="2">Soirée</td>
        </tr>
        <tr>
          <td colspan="2">Horaires</td>
          <td></td>
          <td></td>
          <td></td>
          <td></td>
          <td></td>
          <td></td>

          <td></td>
          <td></td>
          <td></td>
          <td></td>
          <td></td>
          <td></td>
        </tr>
        <tr style="border-bottom : 2px dotted #22A1DC;">
          <td style="transform: rotate(270deg);" rowspan="2">Distance</td>
          <td align="left">Moyenne</td>
          <td colspan="2">{statisticsDay1(MORNING).averageDistance}</td>
          <td colspan="2">{statisticsDay1(EVENING).averageDistance}</td>
          <td colspan="2">{statisticsDay2(MORNING).averageDistance}</td>
          <td colspan="2">{statisticsDay2(EVENING).averageDistance}</td>
          <td colspan="2">{statisticsDay3(MORNING).averageDistance}</td>
          <td colspan="2">{statisticsDay3(EVENING).averageDistance}</td>
        </tr>
        <tr>
          <td align="left">Total</td>
          <td colspan="2">{statisticsDay1(MORNING).totalDistance}</td>
          <td colspan="2">{statisticsDay1(EVENING).totalDistance}</td>
          <td colspan="2">{statisticsDay2(MORNING).totalDistance}</td>
          <td colspan="2">{statisticsDay2(EVENING).totalDistance}</td>
          <td colspan="2">{statisticsDay3(MORNING).totalDistance}</td>
          <td colspan="2">{statisticsDay3(EVENING).totalDistance}</td>
        </tr>

        <tr style="border-bottom : 2px dotted #22A1DC;">
          <td style="transform: rotate(270deg);" rowspan="2">Temps</td>
          <td align="left">Moyenne</td>
          <td colspan="2">{statisticsDay1(MORNING).averageTime}</td>
          <td colspan="2">{statisticsDay1(EVENING).averageTime}</td>
          <td colspan="2">{statisticsDay2(MORNING).averageTime}</td>
          <td colspan="2">{statisticsDay2(EVENING).averageTime}</td>
          <td colspan="2">{statisticsDay3(MORNING).averageTime}</td>
          <td colspan="2">{statisticsDay3(EVENING).averageTime}</td>
        </tr>
        <tr>
          <td align="left">Total</td>
          <td colspan="2">{statisticsDay1(MORNING).totalTime}</td>
          <td colspan="2">{statisticsDay1(EVENING).totalTime}</td>
          <td colspan="2">{statisticsDay2(MORNING).totalTime}</td>
          <td colspan="2">{statisticsDay2(EVENING).totalTime}</td>
          <td colspan="2">{statisticsDay3(MORNING).totalTime}</td>
          <td colspan="2">{statisticsDay3(EVENING).totalTime}</td>
        </tr>
      </table>
    </div>
  }
}
