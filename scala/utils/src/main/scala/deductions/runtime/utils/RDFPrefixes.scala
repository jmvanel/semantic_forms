package deductions.runtime.utils

import java.net.{URI => jURI}

import org.w3.banana.{RDF, RDFOps, RDFPrefix, RDFSPrefix, _}

import scala.util.{Failure, Success, Try}
import scala.jdk.CollectionConverters._
import org.apache.jena.graph.Graph

/** database of Turtle prefixes;
 *  TODO possibility to add prefix-URI pairs;
 *  TODO use prefix.cc like in EulerGUI */
  trait RDFPrefixes[Rdf <: RDF] extends RDFPrefixesInterface {

  implicit val ops: RDFOps[Rdf]
  import ops._

  val commonSchemes = List("http", "https", "url")

  val restruc = Prefix[Rdf]("restruc", "http://deductions.github.io/restruc.owl.ttl#" )
  val tasks = Prefix[Rdf]("tm", "http://deductions.github.io/task-management.owl.ttl#" )

  def uriFromPrefix(pf: String): String = fromUri(prefixesMap(pf))

  private val prefixAV = "http://virtual-assembly.org/pair#" // "http://www.virtual-assembly.org/ontologies/1.0/pair#"

  lazy val rdfs = RDFSPrefix[Rdf]

  // TODO remove private
  private lazy val rdf = RDFPrefix[Rdf]
  private lazy val xsd = XSDPrefix[Rdf]
  private lazy val owl = OWLPrefix[Rdf]

  lazy val foaf = FOAFPrefix[Rdf]
  lazy val skos = Prefix[Rdf]("skos", "http://www.w3.org/2004/02/skos/core#")
  lazy val sioc =     Prefix[Rdf]("sioc", "http://rdfs.org/sioc/ns#")
  lazy val schema = Prefix[Rdf]("schema", "http://schema.org/")
  lazy val text = Prefix[Rdf]("text", "http://jena.apache.org/text#" )
  lazy val dbo = Prefix[Rdf]("dbo", "http://dbpedia.org/ontology/")
  lazy val vs = Prefix[Rdf]("vs", "http://www.w3.org/2003/06/sw-vocab-status/ns#")
  lazy val content = Prefix[Rdf]("content", "http://purl.org/rss/1.0/modules/content/")

  // prefix for RDF data
  lazy val dbpedia = Prefix[Rdf]("dbpedia", "http://dbpedia.org/resource/")
  lazy val wikidata =  Prefix[Rdf]("wikidata", "http://www.wikidata.org/entity/")

  /** vocabulary for forms in general (eg form:showProperties */
  lazy val form = Prefix[Rdf]("form",
      "http://raw.githubusercontent.com/jmvanel/semantic_forms/master/vocabulary/forms.owl.ttl#")

  /** prefix for specific forms (eg foaf-forms:personForm) */
  lazy val foafForms = Prefix[Rdf]("foaf-forms",
    "http://raw.githubusercontent.com/jmvanel/semantic_forms/master/scala/forms/form_specs/foaf.form.ttl#" )

lazy val loginForms = Prefix[Rdf]("login-forms",
"http://raw.githubusercontent.com/jmvanel/semantic_forms/master/scala/forms/form_specs/login.form.ttl#" )

  /** dct: dc/terms/ : properties "intended to be used with non-literal values" */
  lazy val dct = DCTPrefix[Rdf]
  /** dc: dc/elements : no rdfs:range nor domain ; rather not use! */
  lazy val dc = DCPrefix[Rdf]
  lazy val orgVocab = Prefix[Rdf]("org", "http://www.w3.org/ns/org#" )
  lazy val geo = Prefix[Rdf]("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#" )
  lazy val event = Prefix[Rdf]("event", "http://purl.org/NET/c4dm/event.owl#" )

  /** Biodiversity */
  lazy val dwc = Prefix[Rdf]("dwc", "http://rs.tdwg.org/dwc/terms/")
  lazy val karstlink = Prefix[Rdf]("karstlink", "https://ontology.uis-speleo.org/ontology/#" )

  lazy val geoloc = Prefix[Rdf]("geoloc", "http://deductions.github.io/geoloc.owl.ttl#")
  lazy val vehman = Prefix[Rdf]("vehman", "http://deductions.github.io/vehicule-management.owl.ttl#")
  lazy val vehma = Prefix[Rdf]("vehma", "http://deductions.github.io/vehicule-management.owl.ttl#")
  lazy val dcat = Prefix[Rdf]("dcat", "http://www.w3.org/ns/dcat#")
  lazy val void = Prefix[Rdf]("void", "http://rdfs.org/ns/void#" ) // non self-hosted :( !
  lazy val con = Prefix[Rdf]("con", "http://www.w3.org/2000/10/swap/pim/contact#" )

  lazy val orga = Prefix[Rdf]("org", "http://www.w3.org/ns/org#" )

  lazy val prefixesList = List(
		  // prefixes for ontologies
    rdf, rdfs,
    xsd,
    dc, dct,
    foaf, Prefix[Rdf]("doap", "http://usefulinc.com/ns/doap#"),
    LDPPrefix[Rdf], IANALinkPrefix[Rdf], WebACLPrefix[Rdf], CertPrefix[Rdf],
    owl, skos,
    schema,
    Prefix[Rdf]("gr", "http://purl.org/goodrelations/v1#"),
    sioc, content,
    dbo,
    Prefix[Rdf]("vcard", "http://www.w3.org/2006/vcard/ns#"),
    Prefix[Rdf]("ical", "http://www.w3.org/2002/12/cal/ical#"),
    foafForms,  // for specific form specs (FOAF, etc)
    form,   // form vocabulary

    Prefix[Rdf]("pair", prefixAV ),
    restruc,
    tasks,
    Prefix[Rdf]("", "http://data.onisep.fr/ontologies/" ),
    Prefix[Rdf]("doas", "http://deductions.github.io/doas.owl.ttl#"),
    Prefix[Rdf]("bioc", "http://deductions.github.io/biological-collections.owl.ttl#"),
    Prefix[Rdf]("seeds", "http://deductions.github.io/seeds.owl.ttl#"),
    Prefix[Rdf]("nature", "http://deductions.github.io/nature_observation.owl.ttl#"),
    geoloc, vehman,

    Prefix[Rdf]("cco", "http://purl.org/ontology/cco/core#" ),
    geo, event,
    dcat,
    void,
    orga,
    karstlink,

    // prefixes for resources

    dbpedia, wikidata,
    Prefix[Rdf]("wd", "http://www.wikidata.org/entity/"),

    text // Jena text search
    )

  class CCOPrefix(ops: RDFOps[Rdf])
      extends PrefixBuilder("cco", "http://purl.org/ontology/cco/core#")(ops) {
    val expertise = apply("expertise")
  }
  lazy val cco = new CCOPrefix(ops)
  
  class VCardPrefix(ops: RDFOps[Rdf])
      extends PrefixBuilder("vcard", "http://www.w3.org/2006/vcard/ns#" )(ops) {
	  val postal_code = apply("postal-code")
	  val locality =  apply("locality")
  }
  lazy val vcard = new VCardPrefix(ops)//[Rdf]


  /** map from RDF prefix to Rdf#URI */
  lazy val prefixesMap: Map[String, Rdf#URI] =
    prefixesList.map{ pf =>
      // println(s"prefix ${pf.prefixName} : ${pf.prefixIri}")
      pf.prefixName -> URI(pf.prefixIri) }.toMap

  /** map from RDF prefix to Prefix[Rdf] */
  lazy val prefixesMap2: Map[String, Prefix[Rdf]] =
    prefixesList.map{ pf => pf.prefixName -> pf }.toMap
  lazy val urisMap: Map[String, String] =
    prefixesList.map{ pf =>
      (pf.prefixIri) ->
      pf.prefixName
      }.toMap
  lazy val prefix2uriMap = urisMap.map(_.swap)

  lazy val prefix2uriMapMutable = scala.collection.mutable.Map[String, String]() ++ prefix2uriMap
  // prefix2uriMapMutable ++= prefix2uriMap

  def populatePrefix2uriMapFromURL(rdfSource: String) = {
    val gr = org.apache.jena.riot.RDFDataMgr.loadGraph( rdfSource )
    populatePrefix2uriMapJena(gr)
  }

  def populatePrefix2uriMap(graph: Rdf#Graph) = {
    if( graph . isInstanceOf[Graph] ) {
      populatePrefix2uriMapJena( graph.asInstanceOf[Graph] )
  } }
  def populatePrefix2uriMapJena(graph: Graph) = {
     val ret = prefix2uriMapMutable ++= graph.getPrefixMapping.getNsPrefixMap.asScala
     // println( s">>>> populatePrefix2uriMapJena: $ret")
     ret
  }

  def expandOrUnchanged(possiblyPrefixedURI: String): String = {
     val uriMaybe = expand(possiblyPrefixedURI)
     uriMaybe match {
       case Some(uri) => fromUri(uri)
       case None => possiblyPrefixedURI
     }
  }

  def expand(possiblyPrefixedURI: String): Option[Rdf#URI] = {
    expandNEW(possiblyPrefixedURI)
//    expandOLD(possiblyPrefixedURI)
  }

  private def expandOLD(possiblyPrefixedURI: String): Option[Rdf#URI] = {
    val uri_string = possiblyPrefixedURI // URLEncoder.encode(possiblyPrefixedURI, "UTF-8")
    val tr = Try {

      val prefixOption =
        if (possiblyPrefixedURI.startsWith(":")) {
          Some("")
        } else {
          if (uri_string.endsWith(":"))
            Some(uri_string.substring(0, uri_string.length - 1))
          else {
            val uri = new jURI(uri_string)
            if (uri.isAbsolute() && !commonSchemes.contains(uri.getScheme)) {
              // then it's possibly a Prefixed URI like foaf:name
              Some(uri.getScheme)
            } else None
          }
        }

      prefixOption match {
        case Some(prefix) => {
          val prefixAsURI = prefixesMap.get(prefix)
          prefixAsURI match {
            case Some(prefixIri) =>
              Some(URI(fromUri(prefixIri) + possiblyPrefixedURI.substring(prefix.length() + 1)))
            case None => None
          }
        }
        case None => None
      }
    }
    // println(s"expand($possiblyPrefixedURI tr $tr")
    tr match {
      case Success(r) => r
      case Failure(e) => None
    }
  }

  /**
   * expand possibly Prefixed URI (like foaf:name),
   *  @return Some(URI("http://xmlns.com/foaf/0.1/name")),
   *  or output None if no prefix is found
   * use mutable prefix to URI Map
   *
   *  TODO move to URIHelpers
   */
  def expandNEW(possiblyPrefixedURI: String): Option[Rdf#URI] = {
    val uri_string = possiblyPrefixedURI // URLEncoder.encode(possiblyPrefixedURI, "UTF-8")
    val tr = Try {

      val prefixOption =
        if (possiblyPrefixedURI.startsWith(":")) {
          Some("")
        } else {
          if (uri_string.endsWith(":"))
            Some(uri_string.substring(0, uri_string.length - 1))
          else {
            val uri = new jURI(uri_string)
            if (uri.isAbsolute() && !commonSchemes.contains(uri.getScheme)) {
              // then it's possibly a Prefixed URI like foaf:name
              Some(uri.getScheme)
            } else None
          }
        }

      prefixOption match {
        case Some(prefix) => {
          // println(s"prefix2uriMapMutable $prefix2uriMapMutable")
          val prefixAsURI = prefix2uriMapMutable.get(prefix)
          prefixAsURI match {
            case Some(prefixIri) =>
              Some(URI(prefixIri + possiblyPrefixedURI.substring(prefix.length() + 1)))
            case None => None
          }
        }
        case None => None
      }
    }
    // println(s"expand($possiblyPrefixedURI tr $tr")
    tr match {
      case Success(r) => r
      case Failure(e) => None
    }
  }

  def abbreviateTurtle(uri: String): String = {
      abbreviateTurtle(URI(uri))
  }

  /**
   * inverse of #expand()
   *  @return abbreviated Turtle term, eg foaf:name
   */
  def abbreviateTurtle(uri: Rdf#Node): String = {
//    println(s"abbreviateTurtle2($uri")
    val inputURI = uri.toString()
    val found = urisMap.find {
      case (uriMapped, pref) =>
        inputURI.startsWith(uriMapped)
    }
    found match {
      case Some((iri, pref)) =>
        val id = inputURI.substring(iri.length)
//        println(s"abbreviateTurtle2: $pref:$id")
        s"$pref:$id"
      case None =>
//        println(s"abbreviateTurtle2: None")
        inputURI
    }
  }
}
