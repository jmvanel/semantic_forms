package deductions.runtime.abstract_syntax

import org.w3.banana.RDF
import deductions.runtime.dataset.RDFOPerationsDB
import org.w3.banana.Prefix
import org.w3.banana.OWLPrefix
import deductions.runtime.utils.RDFHelpers
import org.w3.banana.RDFSPrefix
import org.w3.banana.RDFPrefix
import deductions.runtime.services.Configuration
import deductions.runtime.services.SPARQLHelpers

/** populate Fields in form by inferencing from class(es) of given instance URI */
trait FieldsInference[Rdf <: RDF, DATASET]
extends RDFHelpers[Rdf]
with RDFOPerationsDB[Rdf, DATASET]
with SPARQLHelpers[Rdf, DATASET]
with Configuration
{
//  import Configuration._

  import ops._
  private val rdfs = RDFSPrefix[Rdf]
  private val owl = OWLPrefix[Rdf]

  def fieldsFromClass(classs: Rdf#URI, graph: Rdf#Graph): Seq[Rdf#Node] = {

    val result = scala.collection.mutable.ListBuffer[Rdf#Node]()

    /** retrieve rdfs:domain's From given Class */
    def domainsFromClass(classs: Rdf#Node): List[Rdf#Node] = {
      if (classs != owl.Thing) {
        val relevantPredicates = getSubjects(graph, rdfs.domain, classs).toSeq
        println(s"""Predicates with domain = Class <$classs> , relevant Predicates size ${relevantPredicates.size}  ; ${relevantPredicates.mkString(", ")}""")
        relevantPredicates.distinct.toList ++
        unionOfDomainsFromClass(classs)
      } else List()
    }

    /**
     * retrieve rdfs:domain's being unionOf from given Class
     *  NOTE: SPARQL query that covers also what domainsFromClass() does
     */
    def unionOfDomainsFromClass(classe: Rdf#Node): List[Rdf#Node] = {
      if (lookup_domain_unionOf) {
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
        println(s"unionOfDomainsFromClass queryString $queryString")
        val res = sparqlSelectQueryVariablesNT(queryString, Seq("PRED"), dataset)
        println(s"unionOfDomainsFromClass res $res")
        res map { li => li.head }
      } else List()
    }

    /** recursively process super-classes and owl:equivalentClass until reaching owl:Thing */
    def processSuperClasses(classs: Rdf#Node) {
      if (classs != owl.Thing) {
        result ++= domainsFromClass(classs)
        val superClasses = getObjects(graph, classs, rdfs.subClassOf)
        println(s"process Super Classes of <$classs> size ${superClasses.size} ; ${superClasses.mkString(", ")}")
        superClasses foreach (sc => result ++= domainsFromClass(sc))
        val equivalentClasses = getObjects(graph, classs, owl.equivalentClass)
        equivalentClasses foreach (sc => result ++= domainsFromClass(sc))
        superClasses foreach (sc => processSuperClasses(sc))
      }
    }

    /**
     * get the ontology prefix,
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

    /**
     * add Domainless Properties;
     * properties without Domain are supposed to be applicable to any class in the same ontology
     *  ( use case : DOAP )
     */
    def addDomainlessProperties(uri: Rdf#URI) {
      val graphURI = getGraphURI(uri)
      val props1 = find(graph, ANY, toConcreteNodeMatch(rdf.typ), toConcreteNodeMatch(rdf.Property))
      val props2 = find(graph, ANY, toConcreteNodeMatch(rdf.typ), toConcreteNodeMatch(owl.ObjectProperty))
      val props3 = find(graph, ANY, toConcreteNodeMatch(rdf.typ), toConcreteNodeMatch(owl.DatatypeProperty))
      val pp = props1 ++ props2 ++ props3
      for (t <- pp) {
        val (prop, _, _) = fromTriple(t)
        //        if( prop.toString.contains("doap") && prop.toString.contains("name"))
        //          println("doap") // debug <<<<<
        if (prop.toString().startsWith(graphURI)) {
          val doms = find(graph, toConcreteNodeMatch(prop), toConcreteNodeMatch(rdfs.domain), ANY)
          if (doms.size == 0) {
            result += prop.asInstanceOf[Rdf#URI]
            println(s"addDomainlessProperties: <$uri> add <$prop>")
          }
        }
      }
    }

    processSuperClasses(classs)
    if (showDomainlessProperties) addDomainlessProperties(classs)
    result.distinct
  }
}