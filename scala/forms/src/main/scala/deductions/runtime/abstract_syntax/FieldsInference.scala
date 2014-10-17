package deductions.runtime.abstract_syntax

import org.w3.banana.RDF

/** populate Fields in form by inferencing from class(es) */
trait FieldsInference [Rdf <: RDF] {
  self : FormSyntaxFactory[Rdf] =>
    
  
  def fieldsFromClass(classs: Rdf#URI, graph: Rdf#Graph): Set[Rdf#URI] = {
    def domainsFromClass(classs: Rdf#Node) = {
      val relevantPredicates = ops.getSubjects(graph, rdfs.domain, classs).toSet
      extractURIs(relevantPredicates) toSet
    }
    
    val result = scala.collection.mutable.Set[Rdf#URI]()

    /** recursively process super-classes until reaching owl:Thing */
    def processSuperClasses(classs: Rdf#URI) {
      result ++= domainsFromClass(classs)
//      if (classs != owl.Thing ) { // for Banana 0.7
      if (classs != ops.makeUri(owlThing) ) {
        val superClasses = ops.getObjects(graph, classs, rdfs.subClassOf)
        superClasses foreach (sc => result ++= domainsFromClass(sc))
      }
    }
    
    /** get the ontology prefix
     *  TODO : related to URI cache */
    def getGraphURI(classs: Rdf#URI) : String = {
     ops.getFragment(classs) match {
      case Some(frag) =>
//        classs.getURI().substring(frag.length() + 1)
        classs.toString().substring(frag.length() + 1)
      case None => ""
    }
  }

    /**
     * Properties without Domain are supposed to be applicable to any class in the same ontology
     *  ( use case : DOAP )
     */
    def addDomainlessProperties(uri: Rdf#URI) {
      val props1 = ops.find(graph, ops.ANY, ops.toConcreteNodeMatch(rdf.typ), ops.toConcreteNodeMatch(rdf.Property))
      val props2 = ops.find(graph, ops.ANY, ops.toConcreteNodeMatch(rdf.typ), ops.toConcreteNodeMatch(owl.ObjectProperty))
      val props3 = ops.find(graph, ops.ANY, ops.toConcreteNodeMatch(rdf.typ), ops.toConcreteNodeMatch(owl.DatatypeProperty))
      val pp = props1 ++ props2 ++ props3
      for (t <- pp) {
        val (prop, _, _) = ops.fromTriple(t)
        val graphURI = getGraphURI(uri)
        if (prop.toString().startsWith(graphURI)) {
          val doms = ops.find(graph, ops.toConcreteNodeMatch(prop), ops.toConcreteNodeMatch(rdfs.domain), ops.ANY)
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