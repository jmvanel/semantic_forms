package deductions.runtime.abstract_syntax

import org.w3.banana.RDF

/** populate Fields in form by inferencing from class(es) of given instance URI */
trait FieldsInference[Rdf <: RDF] {
  self: FormSyntaxFactory[Rdf] =>
  import ops._

  def fieldsFromClass(classs: Rdf#URI, graph: Rdf#Graph): Set[Rdf#URI] = {
    /** retrieve rdfs:domain's From given Class */
    def domainsFromClass(classs: Rdf#Node) = {
      val relevantPredicates = getSubjects(graph, rdfs.domain, classs).toSet
      rdfh.nodeSeqToURISet(relevantPredicates)
    }

    val result = scala.collection.mutable.Set[Rdf#URI]()

    /** recursively process super-classes and owl:equivalentClass until reaching owl:Thing */
    def processSuperClasses(classs: Rdf#URI) {
      result ++= domainsFromClass(classs)
      if (classs != owl.Thing) {
        val superClasses = getObjects(graph, classs, rdfs.subClassOf)
        superClasses foreach (sc => result ++= domainsFromClass(sc))
        val equivalentClasses = getObjects(graph, classs, owl.equivalentClass)
        equivalentClasses foreach (sc => result ++= domainsFromClass(sc))
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
      val props1 = find(graph, ANY, toConcreteNodeMatch(rdf.typ), toConcreteNodeMatch(rdf.Property))
      val props2 = find(graph, ANY, toConcreteNodeMatch(rdf.typ), toConcreteNodeMatch(owl.ObjectProperty))
      val props3 = find(graph, ANY, toConcreteNodeMatch(rdf.typ), toConcreteNodeMatch(owl.DatatypeProperty))
      val pp = props1 ++ props2 ++ props3
      for (t <- pp) {
        val (prop, _, _) = fromTriple(t)
        //        if( prop.toString.contains("doap") && prop.toString.contains("name"))
        //          println("doap") // debug <<<<<
        val graphURI = getGraphURI(uri)
        if (prop.toString().startsWith(graphURI)) {
          val doms = find(graph, toConcreteNodeMatch(prop), toConcreteNodeMatch(rdfs.domain), ANY)
          if (doms.size == 0) {
            result += prop.asInstanceOf[Rdf#URI]
            println("addDomainlessProperties: add " + prop)
          }
        }
      }
    }

    processSuperClasses(classs)
    addDomainlessProperties(classs)
    result.toSet
  }
}