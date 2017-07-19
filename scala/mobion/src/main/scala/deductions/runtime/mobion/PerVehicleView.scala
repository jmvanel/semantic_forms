package deductions.runtime.mobion

import deductions.runtime.abstract_syntax.{ FormSyntaxFactory, FormSyntaxFromSPARQL }
import deductions.runtime.sparql_cache.dataset.RDFStoreLocalProvider
import deductions.runtime.html.TableView
import deductions.runtime.sparql_cache.algos.GeoPath
import deductions.runtime.utils.RDFPrefixesInterface
import deductions.runtime.core.HTTPrequest
import deductions.runtime.core.SemanticController

import org.w3.banana.RDF

import scala.xml.NodeSeq
import deductions.runtime.abstract_syntax.uri.URIForDisplayFactory

/** Per Vehicle View */
trait PerVehicleView[Rdf <: RDF, DATASET] extends GeoPath[Rdf, DATASET]
    with SemanticController
    with FormSyntaxFactory[Rdf, DATASET]
    with FormSyntaxFromSPARQL[Rdf, DATASET]
    with TableView[Rdf#Node, Rdf#URI]
    with RDFPrefixesInterface
    with URIForDisplayFactory[Rdf, DATASET] {

  import ops._

  def result(request: HTTPrequest): NodeSeq = {

    val vehicles = enumerateVehicles()
    
    showVehicles(vehicles, request)
    
    // 2) compute details for each vehicle

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

    // 3) layouts details for each vehicle
    val template =
      <div class="tab_global_bas">
        <h2>Détail d'activité de la semaine du ../../.. au ../../..</h2>
        <p>
          CRUIS RENT vous donne le détail de votre activité de livraison par service
           lors de la semaine d'évaluation
        </p>
        <!-- TABLEAU RESULTATS DÉTAILLÉS-->
        <table cellpadding="10" width="90%">
          <tr>
            <th colspan="2"></th>
            <th colspan="4">Jour 1 - Date</th>
            <th colspan="4">Jour 2 - Date</th>
            <th colspan="4">Jour 3 - Date</th>
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
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
          </tr>
          <tr>
            <td align="left">Total</td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
          </tr>
          <tr style="border-bottom : 2px dotted #22A1DC;">
            <td style="transform: rotate(270deg);" rowspan="2">Temps</td>
            <td align="left">Moyenne</td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
          </tr>
          <tr>
            <td align="left">Total</td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
          </tr>
        </table>
        <table cellpadding="10" width="90%">
          <tr>
            <th colspan="2"></th>
            <th colspan="4">Jour 4 - Date</th>
            <th colspan="4">Jour 5 - Date</th>
            <th colspan="4">Jour 6 - Date</th>
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
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
          </tr>
          <tr>
            <td align="left">Total</td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
          </tr>
          <tr style="border-bottom : 2px dotted #22A1DC;">
            <td style="transform: rotate(270deg);" rowspan="2">Temps</td>
            <td align="left">Moyenne</td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
          </tr>
          <tr>
            <td align="left">Total</td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
            <td colspan="2"></td>
          </tr>
        </table>
        <!-- TABLEAU RESULTATS DÉTAILLÉS-->
      </div>
    ???
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
      |} WHERE {
      | GRAPH ?GR {
      |   ?MOB a geoloc:Mobile .
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
    vehicles . map { vehicle => <p>
      {
        // TODO hyperlink
        makeURIForDisplay(vehicle)( allNamedGraph, request.getLanguage() )
      } </p> }
  }

}
