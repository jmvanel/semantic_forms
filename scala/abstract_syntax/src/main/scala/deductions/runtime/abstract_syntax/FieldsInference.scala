package deductions.runtime.abstract_syntax

import deductions.runtime.core.FormModule
import deductions.runtime.sparql_cache.SPARQLHelpers
import deductions.runtime.sparql_cache.dataset.RDFOPerationsDB
import deductions.runtime.utils.{Configuration, RDFHelpers}
import org.w3.banana.{OWLPrefix, Prefix, RDF}

/** populate Fields in form by inferencing from given class, using ontologies and properties:
 *  - rdfs:subClassOf
 *  - rdfs:domain
 *  */
trait FieldsInference[Rdf <: RDF, DATASET]
extends RDFHelpers[Rdf]
with RDFOPerationsDB[Rdf, DATASET]
with SPARQLHelpers[Rdf, DATASET]
with FormModule[Rdf#Node, Rdf#URI]
{

	val config: Configuration
  import config._
  import ops._
  private val owl = OWLPrefix[Rdf]

  /** find fields from given Instance subject */
  def fieldsFromSubject(subject: Rdf#Node, graph: Rdf#Graph): Seq[Rdf#URI] =
    getPredicates(graph, subject).toSeq.distinct

  /** find fields from given RDF class */
  def fieldsFromClasses(classes:  List[Rdf#Node], subject: Rdf#Node, editable: Boolean, graph: Rdf#Graph)
  : List[FormSyntax] =
	  for( classs <- classes) yield {
	    val ff = fieldsFromClass( uriNodeToURI(classs), graph)
	    ff.setSubject(subject, editable)
	  }

  /** find fields from given RDF class */
  def fieldsFromClass(classs: Rdf#URI, graph: Rdf#Graph)
  : FormSyntax
  = {

    val inferedProperties = scala.collection.mutable.ListBuffer[Rdf#Node]()
    val propertiesGroups = scala.collection.mutable.HashMap.empty[Rdf#Node,FormSyntax]
    /* retrieve properties from rdfs:domain's From given Class */
    def propertiesFromDomainsFromClass(classs: Rdf#Node): List[Rdf#Node] = {
      if (classs != owl.Thing) {
        val relevantPredicates = getSubjects(graph, rdfs.domain, classs).toSeq
        logger.debug(s"""Predicates with domain = Class <$classs> , relevant Predicates size ${relevantPredicates.size}  ; ${relevantPredicates.mkString(", ")}""")
        filterObsoletePredicates(relevantPredicates.distinct).toList ++
        unionOfDomainsFromClass(classs)
      } else List()
    }

    def filterObsoletePredicates(predicates: Seq[Rdf#Node]): Seq[Rdf#Node] = {
      val vs = Prefix[Rdf]("vs", "http://www.w3.org/2003/06/sw-vocab-status/ns#")
      predicates.filter {
        predicate =>
          val status = getObjects(graph, predicate, vs("term_status")).toSeq
          ! status.contains(Literal("archaic"))
      }
      // vs:term_status "archaic"
      // @prefix vs: <http://www.w3.org/2003/06/sw-vocab-status/ns#> .
    }

    /* retrieve rdfs:domain's being unionOf from given Class
     *  NOTE: SPARQL query that covers also what domainsFromClass() does
     */
    def unionOfDomainsFromClass(classe: Rdf#Node): List[Rdf#Node] = {
      if (lookup_domain_unionOf) {
        // TODO store compiled query
        val queryString = s"""
        ${declarePrefix(owl)}
        ${declarePrefix(rdfs)}
        prefix list: <http://jena.hpl.hp.com/ARQ/list#>
        SELECT ?PRED
        WHERE {
          GRAPH ?G {
            ?PRED rdfs:domain ?UCLASS .
            ?UCLASS owl:unionOf ?UNION .
            # NOTE: rdfs:member Jena ARQ specific :(
            # rdfs:member
            ?UNION
            list:member
            <$classe>
          .
          }
        } """
        logger.debug(s"unionOfDomainsFromClass queryString $queryString")
        val res = sparqlSelectQueryVariablesNT(queryString, Seq("PRED"), dataset)
        logger.debug(s"unionOfDomainsFromClass res $res")
        res map { li => li.head }
      } else List()
    }

    /* recursively process super-classes and owl:equivalentClass until reaching owl:Thing */
    def processSuperClasses(classs: Rdf#Node) {
      if (classs != owl.Thing) {
        val domains = propertiesFromDomainsFromClass(classs)
        inferedProperties ++= domains
        propertiesGroups += ( classs -> FormSyntax(URI(""),Seq(),makeEntries(domains),classs) )

        val superClasses = getObjects(graph, classs, rdfs.subClassOf)
        logger.info(s"process Super Classes of <$classs> size ${superClasses.size} ; ${superClasses.mkString(", ")}")
        superClasses foreach (sc => inferedProperties ++= propertiesFromDomainsFromClass(sc))
        val equivalentClasses = getObjects(graph, classs, owl.equivalentClass)
        equivalentClasses foreach (sc => inferedProperties ++= propertiesFromDomainsFromClass(sc))
        superClasses foreach (sc => processSuperClasses(sc))
      }
    }

    /* get the ontology prefix,
     * taking in account if it ends with #, or with / like FOAF .
     *  TODO : related to URI cache
     */
    def getGraphURI(classs: Rdf#URI): String = {
      getFragment(classs) match {
        case Some(frag) =>
          //        classs.toString().substring(frag.length() + 1)
          withoutFragment(classs) + "#"
        case None =>
          val i = classs.toString().lastIndexOf("/")
          if (i > 0)
            classs.toString().substring(0, i)
          else classs.toString()
      }
    }

    /* add Domainless Properties;
     * properties without Domain are supposed to be applicable to any class in the same ontology
     *  ( use case : DOAP )
     */
    def addDomainlessProperties(uri: Rdf#URI) {
      val graphURI = getGraphURI(uri)
      val props1 = find(graph, ANY, toConcreteNodeMatch(rdf.typ), toConcreteNodeMatch(rdf.Property))
      val props2 = find(graph, ANY, toConcreteNodeMatch(rdf.typ), toConcreteNodeMatch(owl.ObjectProperty))
      val props3 = find(graph, ANY, toConcreteNodeMatch(rdf.typ), toConcreteNodeMatch(owl.DatatypeProperty))
      val allProperties = props1 ++ props2 ++ props3
      for (t <- allProperties) {
        val (prop, _, _) = fromTriple(t)
        //        if( prop.toString.contains("doap") && prop.toString.contains("name"))
        //          println("doap") // debug <<<<<
        if (prop.toString().startsWith(graphURI)) {
          val doms = find(graph, toConcreteNodeMatch(prop), toConcreteNodeMatch(rdfs.domain), ANY)
          if (doms.size == 0) {
            inferedProperties += prop.asInstanceOf[Rdf#URI]
            logger.info(s"addDomainlessProperties: <$uri> add <$prop>")
          }
        }
      }
    }

    processSuperClasses(classs)
    if (showDomainlessProperties) addDomainlessProperties(classs)

    FormSyntax(
        nullURI,
        Seq(),
        makeEntries( inferedProperties.distinct ),
        classs,
        nullURI,
        propertiesGroupMap = propertiesGroups)

  } // end of fieldsFromClass()
}
