package deductions.runtime.connectors

import org.w3.banana.RDF
import deductions.runtime.utils.RDFPrefixes
import org.w3.banana.PrefixBuilder
import org.w3.banana.RDFOps
import org.w3.banana.Prefix

trait CSVmappings[Rdf <: RDF]	extends RDFPrefixes[Rdf] {

	implicit val ops: RDFOps[Rdf]
  import ops._
  
    /** columns Mappings from column names to well-known RDF properties
   *  TODO
   *  make this Map an argument
   *  use labels on properties to propose properties to user,
   *  manage prefixes globally, maybe using prefix.cc */
  lazy val columnsMappings = Map(

      // AV, Guillaume's column names

      "Prénom" -> foaf.givenName,
      "Nom" -> foaf.familyName,
      "organisation 1" -> av.contributesToOrganization,
      "organisation 2" -> av.contributesToOrganization,

      // foaf:Organization or foaf:Person     
      "Email" -> foaf.mbox,
      "Téléphone" -> foaf.phone,
      "Catégorie" -> foaf.focus,
      "Code postal" -> vcard.postal_code,
	    "Ville" -> vcard.locality,
	    "Description courte" -> foaf.name,
	    "Description Longue" -> rdfs.comment,
	    "Site Web" -> foaf.homepage,
	    
      "Projet 1" -> foaf.currentProject,
      "Projet 2" -> foaf.currentProject,
      "Projet 3" -> foaf.currentProject,
      
      "compétence 1" -> cco.expertise,
      "compétence 2" -> cco.expertise,
      "autre" -> cco.expertise,
      
      "Idée 1" -> av.idea,
      "Idée 2" -> av.idea,
      "Rencontré à" -> av.metAt,

      // GV (gvoi: prefix)

      "n° arrivee" -> gvoi("arrivalNumber"),
      "Nom structure pour administration" -> gvoi("administrativeName"), // foaf.name,
      "Nom pour communication" ->  foaf.name, // URI("http://dbpedia.org/ontology/longName"),

      "Prénom interlocuteur référent" -> foaf.givenName,
      "Nom interlocuteur référent" -> foaf.familyName,
      "Numéro contact" -> foaf.phone,
      "Adresse e-mail" -> foaf.mbox,

      "Page Facebook" -> gvoi("facebook"),
      "Compte twitter" -> gvoi("twitter"),
      "Linkedin" -> gvoi("linkedin"),
      "facebook" -> gvoi("facebook"),

      "SUIVI" -> gvoi("status"),
      "Thématiques" -> foaf.status,
      "Activité" -> gvoi("description"), // dc("subject"),
      "Bâtiment" -> gvoi("building"),
      "Espace" -> gvoi("room"),
      "Arrivée" -> gvoi("arrivalDate"),
      "Contribution au projet proposée" -> gvoi( "proposedContribution"),
      "Contribution réalisée" -> gvoi( "realisedContribution"),
      "Nombre salariés" -> gvoi( "employeesCount"),
      "NB de mentions presses" -> gvoi( "pressReferencesCount"),
      "Structure juridique signature convention" -> gvoi( "conventionType"),
      "Numéro de clé au PC" -> gvoi("keyNumber"),
      "Propose du Bénévolat" -> gvoi("volunteeringProposals"),
      "Projets au sein des GV" -> foaf.currentProject,
      "URL logo" -> foaf.img,
      "Assurance 2017" -> gvoi("insuranceStatus"), // xsd.boolean,
      "Raisons du départ" -> gvoi("departureReasons"),

      /* n° arrivee,SUIVI,SITE WEB,PRINT,Nom structure pour administration,Nom pour communication,Prénom interlocuteur référent,
       * Nom interlocuteur référent,Slack,Numéro contact,Adresse e-mail,Prénom interlocuteur référent 2,
       * Nom interlocuteur référent 2,Numéro contact 2,Adresse e-mail 2,Activité,Site Web,Bâtiment,Espace,Arrivée,
       * Contribution au projet proposée,Contribution réalisée,Nombre salariés,
       * facebook,Compte twitter,
       * "Linkedin",NB de mentions presses,Ventes ou services,Structure juridique signature convention,
       * Numéro de clé au PC,Propose du Bénévolat,Projets au sein des GV,URL logo,Assurance 2017,N° contrat Link,Thématiques 

n° arrivee	SUIVI	SITE WEB	PRINT	Nom structure pour administration	Nom pour communication	Prénom interlocuteur référent	Nom interlocuteur référent	
Slack	Numéro contact	Adresse e-mail	Prénom interlocuteur référent 2	Nom interlocuteur référent 2	Numéro contact 2	Adresse e-mail 2	
Activité	Site Web	Bâtiment	Espace	Arrivée	Contribution au projet proposée	Contribution réalisée	Nombre salariés	facebook	Compte twitter	"Linkedin"	
NB de mentions presses	Ventes ou services	Structure juridique signature convention	Numéro de clé au PC	Propose du Bénévolat	Projets au sein des GV	URL logo	Assurance 2017	N° contrat Link	Thématiques 	Raisons du départ
       */
      
      // Tasks management

      "Tâche" ->  rdfs.label,
      "Etat" ->  tasks("state"),
      "JH estimé" ->  tasks("workDurationEstimated"),
      "JH réel" ->  tasks("workDuration"),
      "Assignée à" ->  tasks("assignee"),
      "Suivi par" ->  tasks("managedBy"),
      "Réalisée par" ->  tasks("realizedBy"),
      "TJM" ->  tasks(""),  // ???
      "Tarif estimé" ->  tasks("estimatedPrice"),  // ???
      "Tarif réel" ->  tasks("price"),
      "Détail" ->  rdfs.comment,
      "Discussion" ->  tasks("discussion"),
      "Groupe" ->  tasks("groupe"),  // ???
      "Priorité" ->  tasks("priority"),  // ???
      "Technologie" ->  tasks("technology"),
      "Difficulté" ->  tasks("hardness"),  // ???

      // ONISEP

      /* For DuplicateCleanerSpecificationApp; NOTE: any rdf:type can be replaced,
      it's not necessarily properties */
      "Identifiant de la propriété" -> URI(restruc.prefixIri + "property"),
      "Id" -> URI(restruc.prefixIri + "property"),
      "Action" -> URI(restruc.prefixIri + "replacingProperty"),

          "Nouveau libellé" -> rdfs.label,
          "Libellé" -> rdfs.label,
          "Propriété à renommer" -> rdfs.label,
          "Commentaire fusion" -> rdfs.comment,

    // TAXREF => Darwin Core

    "Numéro nomenclatural" -> dwc("TAXREF-num"),
    "Numéro nomenclatural du nom retenu" -> dwc("TAXREF-num-ret"),
    "Numéro taxonomique" -> dwc(""),
    "Numéro INPN" -> dwc("INPN-num"),
    // "C ode rang" -> dwc(""),
    "Famille (APG III)" -> dwc("family"),
    // "Nom avec auteur" -> dwc(""),
    // "Année et bibliographie" -> dwc(""),
    "Nom retenu avec auteur" -> foaf.name,
    //"Présent dans Taxref" -> dwc(""),
    "Permalien" -> rdfs.seeAlso,
    "Genre" -> dwc("genus"),
    "Epithète espèce" -> dwc("specificEpithet")
    
    // mobile geo tracking
    // Opérateur	Forfait	Date achat	Identifiant	Pass	Telephone	SIM	PUK	PIN	
    // Déblocage	Installé	Scooter N°	IMEI															
    ,
    "IMEI" -> geoloc("mobile"), // TODO add URI instance prefix imei:
    "Scooter N°" -> vehman("internalNumber"),
    "Telephone" -> foaf.phone,
    "Installé" -> geoloc("activated")
    
    // inventory garden, arboretum
    
  )

    /** lots of boiler plate for vocabularies !!!!!!!!!!! */
  val prefixAVontology = "http://www.virtual-assembly.org/ontologies/1.0/pair#"
  class AVPrefix(ops: RDFOps[Rdf]) extends PrefixBuilder("av", prefixAVontology )(ops) {
      val idea = apply("idea")
      val contributesToOrganization = apply("contributesToOrganization")
      val metAt = apply("metAt")
  }
  private object AVPrefix extends AVPrefix(ops){ def apply = new AVPrefix(ops) }
  private lazy val av = AVPrefix

  private lazy val gvoi = Prefix[Rdf]("gvoi", "http://assemblee-virtuelle.github.io/grands-voisins-v2/gv.owl.ttl#")


}