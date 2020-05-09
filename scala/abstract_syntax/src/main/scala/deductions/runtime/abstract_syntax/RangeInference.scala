package deductions.runtime.abstract_syntax

import deductions.runtime.core.FormModule
import deductions.runtime.utils.RDFOPerationsDB
import deductions.runtime.utils.{RDFHelpers, Timer}
import org.w3.banana.{OWLPrefix, PointedGraph, RDF, SparqlEngine, SparqlOps}

import scala.collection.{Iterable, Seq, Set, mutable}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/**
 * populate Fields in form by inferring possible values from given rdfs:range's URI,
 *  through owl:oneOf and know instances
 */
trait RangeInference[Rdf <: RDF, DATASET]
    extends RDFOPerationsDB[Rdf, DATASET]
    with RDFHelpers[Rdf]
    with InstanceLabelsInferenceMemory[Rdf, DATASET]
    with FormModule[Rdf#Node, Rdf#URI]
    with PossibleValues[Rdf]
    with Timer {

  implicit val sparqlGraph: SparqlEngine[Rdf, Try, Rdf#Graph]
  implicit val sparqlOps: SparqlOps[Rdf]
  
  import ops._
  import sparqlGraph.sparqlEngineSyntax._
  import sparqlOps._

  /** add Possible Values to all entry Fields */
  def addAllPossibleValues(
      formSyntax: FormSyntax,
      valuesFromFormGroup: Seq[(Rdf#Node, Rdf#Node)])(
                               implicit graph: Rdf#Graph,
                               lang: String = "en") = {
    logger.debug( s">>>> addAllPossibleValues: fields.size=${formSyntax.fields.size}")
    for (field <- formSyntax.fields) {
    	val ranges = objectsQuery( field.property, rdfs.range)
    	logger.debug( ">>>> addAllPossibleValues: before addPossibleValues field " + field )
      formSyntax.possibleValuesMap.put(field.property,
        addPossibleValues(field, ranges, valuesFromFormGroup))
      logger.debug( "> field after  addPossibleValues" + field )
    }
  }

  /** add Possible Values to given entry Field */
  private def addPossibleValues(
    entryField: Entry,
    ranges: Set[Rdf#Node],
    valuesFromFormGroup: Seq[(Rdf#Node, Rdf#Node)])
  (implicit graph: Rdf#Graph,
      lang: String = "en" )
    : Seq[(Rdf#Node, Rdf#Node)]  = {

    val owl = OWLPrefix[Rdf]
    val rdfh: RDFHelpers[Rdf] = this

    /* modify entry to populate possibleValues,
     * by taking ?LIST from triples:
     * ?RANGE owl:oneOf ?LIST
     */
    def populateFromOwlOneOf(): Seq[ResourceWithLabel[Rdf]] = {
      // TODO: use yield instead in fillPossibleValuesFromList()
      val possibleValuesFromOwlOneOf = mutable.ArrayBuffer[(Rdf#Node, Rdf#Node)]()
      
      for (range <- ranges) {
        val enumerated = getObjects(graph, range, owl.oneOf)
        fillPossibleValuesFromList(enumerated, possibleValuesFromOwlOneOf)
      }
      logger.debug(s"populateFromOwlOneOf size ${possibleValuesFromOwlOneOf.size} ranges $ranges - entryField $entryField")
      if (!possibleValuesFromOwlOneOf.isEmpty) {
        /* normally we have a non empty list of possible values to propose to user,
         * and then there is no open Choice for her. */
        logger.debug(s"populateFromOwlOneOf ${entryField.label} set openChoice = false")
        // entryField.openChoice = false // TODO <<<<<<<<<<
      }

      /* fill Possible Values into `possibleValues`
       * from given RDF List `enumerated`, which typically comes
       *  from existing triples with relevant rdf:type
       */
      def fillPossibleValuesFromList(
        enumerated0: Iterable[Rdf#Node],
        possibleValues: mutable.ArrayBuffer[(Rdf#Node, Rdf#Node)]): Unit = {
        def combineNodesLabels( seq: Seq[Rdf#Node]) = {
          val list = seq.toList
          val il = instanceLabels(list, lang)
          logger.debug(s"combineNodesLabels: lang $lang, il $il")
          list zip ( il.map{ s => makeLiteral(s, xsd.string) })
        }
        /* NOTE: Iterable are like kleenex: must NOT use them twice;
         * in this case do a copy by toList. */
        val enumerated = enumerated0.toList
        logger.debug(s"fillPossibleValuesFromList: enumerated: $enumerated")
        for (enum <- enumerated)
          foldNode(enum)(
            uri => {
              val list = (rdfh.rdfListToSeq(Some(uri)))
              logger.debug(s"fillPossibleValuesFromList: uri $uri: combineNodesLabels(list) = ${combineNodesLabels(list)}")
              possibleValues.appendAll(
                  combineNodesLabels( list ))
            },
            x => {
              logger.debug(s"fillPossibleValuesFromList: bnode $x")
              val list = rdfh.rdfListToSeq(Some(x))
              possibleValues.appendAll(
                  combineNodesLabels( list ))
            },
            x => { logger.debug(s"fillPossibleValuesFromList: lit $x"); () })
      }

      possibleValuesFromOwlOneOf.map { (couple: (Rdf#Node, Rdf#Node)) =>
        new ResourceWithLabel(couple._1, couple._2)
      }
    }

    /** process owl:union appearing as rdfs:range of a property */
    def populateFromOwlUnion(): Map[ Rdf#Node, Seq[ResourceWithLabel[Rdf] ]] = {
      val classes = processOwlUnion()
      logger.debug( "populateFromOwlUnion: classes " + classes )
      val map = ( for( classe <- classes ) yield {
    	  classe -> tuples2ResourcesWithLabel( getInstancesAndLabels(classe) )
      } ) . toMap
      map
    }
    
    import org.w3.banana.binder._

    /* process owl:union appearing as rdfs:range of a property
     * Typical declaration processed:
     *
     * :implies a owl:ObjectProperty ;
     * rdfs:label "implique"@fr ;
     * rdfs:domain :Idea ;
     * rdfs:range [owl:unionOf(:Problem :Opinion :Effect :Proposition)] .
     */
    def processOwlUnion(): Seq[Rdf#Node] = {
      if (!(ranges isEmpty)) {
        val rangeClass = ranges.head // TODO several range values <<<<<<<<
        
        // TODO call processUnionOf(graph, rangeClass): Seq[Rdf#Node]
        val rdfLists = getObjects(graph, rangeClass, owl.unionOf)
        val binder = PGBinder[Rdf, List[Rdf#Node]]
        if (!(rdfLists isEmpty)) {
          val rdfList = rdfLists.head
          val classesTry = binder.fromPG(PointedGraph(rdfList, graph))
          classesTry match {
            case Success(classes) => classes.toSeq
            case Failure(e)       => Seq()
          }
        } else Seq()
        
      } else Seq()
    }
    
    /*
     * given a list of URI's,
       * @return Possible Values into Seq of pairs (nodeId, label),
       * from given list `enumerated`, which typically comes
       *  from existing triples with relevant rdf:type
       */
      def fillPossibleValues(enumerated: Iterable[Rdf#Node] ):
        Seq[(Rdf#Node, Rdf#Node)] = {
        val uriAndInstanceLabels = enumerated.toList.map {
          enum =>
            foldNode(enum)(
//              uri => (uri, instanceLabelFromTDB(uri, lang)),
//              x => (x, instanceLabelFromTDB(x, lang)),
              uri => (uri, makeInstanceLabel(uri, graph, lang)),
              x => (x, makeInstanceLabel(x, graph, lang)),
              x => (x, ""))
        }
//        logger.debug(s"""fillPossibleValues uriAndInstanceLabels class ${uriAndInstanceLabels.getClass} size """ )
//        logger.debug( uriAndInstanceLabels.size)
        val sortedInstanceLabels = uriAndInstanceLabels.sortBy { e => e._2 }
          sortedInstanceLabels.map {
        	  c => (c._1, makeLiteral(c._2, xsd.string))
          }
      }
      
    /* populate possible Values From Instances */
    def populateFromInstances(): Seq[ResourceWithLabel[Rdf]] = {
      val possibleValues = time(
    		s"populateFromInstances ${entryField.label}",
        { val rrrr = for (rangeClass <- ranges.toSeq ) yield {
          // TODO limit number of possible values; later implement Comet on demand access to possible Values
          getInstancesAndLabels(rangeClass)
        }
        rrrr.flatten
        }
      )
      logger.debug(s"  possibleValues.size ${entryField.property} ${possibleValues.size}" )
      val result = possibleValues.map {
        (couple: (Rdf#Node, Rdf#Node)) =>
        new ResourceWithLabel(couple._1, couple._2)
      }   
      result
    }

    def getInstancesAndLabels(rangeClass: Rdf#Node): Seq[(Rdf#Node, Rdf#Node)] = {
      if (rangeClass != owl.Thing && rangeClass != rdfs.Literal) {
        val enumerated = getSubjects(graph, rdf.typ, rangeClass).toList
//        logger.debug( s"getInstancesAndLabels rangeClass $rangeClass size " + enumerated.size )
        val possibleValues2 = fillPossibleValues(enumerated)
        val subClasses = getSubjects(graph, rdfs.subClassOf, rangeClass) . toList
        val r = for (subClass <- subClasses) yield {
          val subClassesValues = getSubjects(graph, rdf.typ, subClass).toList
//          logger.debug( s"getInstancesAndLabels subClass $subClass size " + enumerated.size )
          val possibleValues3 = fillPossibleValues(subClassesValues)
          possibleValues3
        }
        val rr = possibleValues2 ++ r.flatten.toList
        rr
      } else Seq()
    }
    
    /* populate From configuration in TDB
     *  TODO ? merge given possibleValues with existing ones
     */
    def populateFromTDB(): Seq[ResourceWithLabel[Rdf]] = {
      if (!valuesFromFormGroup.isEmpty) {
//        entryField.openChoice = false // TODO <<<<<<<<<<<<<<
      }
      valuesFromFormGroup.map { (couple: (Rdf#Node, Rdf#Node)) =>
        new ResourceWithLabel(couple._1, couple._2)
      }
    }

    /* this function does the job! */
    def setPossibleValues() : Seq[(Rdf#Node, Rdf#Node)] = {
      val fieldType = entryField.type_
      val possibleValues = {
        
        /* commented out to allow for re-computing each time 
         * the possible values;
         * use cases:
         * - adding a new URI value
         * - updating "label" properties of an URI */
        
//        if (isDefined(fieldType) ) {
//          // to set openChoice = false - TODO this recomputes label already memorized 
//          populateFromOwlOneOf()
//          getPossibleValuesAsTuple(fieldType)
//        } else
        {
          val resourcesWithLabelFromOwlUnion = populateFromOwlUnion()
          val resourcesWithLabel =
            populateFromTDB() ++
              populateFromInstances() ++
              populateFromOwlOneOf()
          val res = recordPossibleValues(resourcesWithLabel, resourcesWithLabelFromOwlUnion )
          res
        }
      }
      possibleValues
    }

    /* record Possible Values */
    def recordPossibleValues(resourcesWithLabel: Seq[ResourceWithLabel[Rdf]],
                             classesAndLabels:  Map[Rdf#Node, Seq[ ResourceWithLabel[Rdf]]]
    ): Seq[(Rdf#Node, Rdf#Node)] = {
      val fieldType = entryField.type_
      if (classesAndLabels isEmpty)
        addPossibleValues(firstNodeOrElseNullURI(fieldType), resourcesWithLabel)
      else {
        for( (classe, rlabels ) <- classesAndLabels ) {
        	addPossibleValues(classe, rlabels)
        }
        val r = classesAndLabels.values.flatMap( identity ) .toSeq
        resourcesWithLabel2Tuples(r)
      }
    }
    
    // ==== body of function addPossibleValues ====

    setPossibleValues
  }

  // ========


  /** @return list of VALUE & LABEL */
  def possibleValuesFromFormGroup(formGroup: Rdf#URI,
    graph1: Rdf#Graph)
    (implicit graph: Rdf#Graph)
    : Seq[(Rdf#Node, Rdf#Node)] = {
    val q = s"""
      ${declarePrefix(form)}
      ${declarePrefix(rdfs)}
              SELECT ?VALUE ?LABEL
              WHERE {
                <${formGroup}> form:labelsForFormGroup ?LABELS .
                ?LABELS form:labelsForValues ?BN .
                ?BN form:value ?VALUE ; rdfs:label ?LABEL . 
              } """
    //    info(s"populateFromTDB $q")

    val query = parseSelect(q, Seq()).get
    val solutions: Rdf#Solutions = graph.executeSelect(query).get

    val res = solutions.iterator() map {
      row =>
        (row("VALUE").get, // .as[Rdf#Node].get,
         row("LABEL").get // .as[Rdf#Node].get
         )
    }
    val possibleValues = res.to[List]
    logger.debug(
        s"""possibleValuesFromFormGroup populateFromTDB formGroup <$formGroup> size ${possibleValues.size}
             ${possibleValues.mkString("\n")}""")
    possibleValues
  }
  
}
