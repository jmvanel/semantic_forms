package deductions.runtime.abstract_syntax

import scala.collection.Iterable
import scala.collection.Seq
import scala.collection.Set
import scala.collection.mutable
import scala.util.Try
import org.w3.banana.OWLPrefix
import org.w3.banana.RDF
import org.w3.banana.RDFSPrefix
import org.w3.banana.SparqlEngine
import org.w3.banana.SparqlOps
import deductions.runtime.dataset.RDFOPerationsDB
import deductions.runtime.services.SPARQLHelpers
import deductions.runtime.utils.RDFHelpers
import deductions.runtime.utils.Timer
import scala.util.Success
import scala.util.Failure
import scala.language.postfixOps
import org.w3.banana.PointedGraph

/**
 * populate Fields in form by inferring possible values from given rdfs:range's URI,
 *  through owl:oneOf and know instances
 */
trait RangeInference[Rdf <: RDF, DATASET]
    extends RDFOPerationsDB[Rdf, DATASET]
    with RDFHelpers[Rdf]
    with InstanceLabelsInferenceMemory[Rdf, DATASET]
    //with InstanceLabelsInference2[Rdf]
    with FormModule[Rdf#Node, Rdf#URI]
    with PossibleValues[Rdf]
    with SPARQLHelpers[Rdf, DATASET]
    with Timer {

  implicit val sparqlGraph: SparqlEngine[Rdf, Try, Rdf#Graph]
  implicit val sparqlOps: SparqlOps[Rdf]
  private val rdfs = RDFSPrefix[Rdf]
  
  import ops._
  import sparqlOps._
  import sparqlGraph._
  import sparqlGraph.sparqlEngineSyntax._

  /** add Possible Values to all entry Fields */
  def addAllPossibleValues(formSyntax: FormSyntax,
                           valuesFromFormGroup: Seq[(Rdf#Node, Rdf#Node)])(implicit graph: Rdf#Graph) = {
    for (field <- formSyntax.fields) {
    	val ranges = objectsQuery( field.property, rdfs.range)
      formSyntax.possibleValuesMap.put(field.property,
        addPossibleValues(field, ranges, valuesFromFormGroup))
    }
  }

  /** add Possible Values to given entry Field */
  def addPossibleValues(
    entryField: Entry,
    ranges: Set[Rdf#Node],
    valuesFromFormGroup: Seq[(Rdf#Node, Rdf#Node)])
  (implicit graph: Rdf#Graph )
//  : Entry
    : Seq[(Rdf#Node, Rdf#Node)]
  = {

    val owl = OWLPrefix[Rdf]
    val rdfh: RDFHelpers[Rdf] = this

    /**
     * modify entry to populate possibleValues,
     * by taking ?LIST from triples:
     * ?RANGE owl:oneOf ?LIST
     */
    def populateFromOwlOneOf(): Seq[ResourceWithLabel[Rdf]] = {
      val possibleValues = mutable.ArrayBuffer[(Rdf#Node, Rdf#Node)]()
      for (range <- ranges) {
        val enumerated = getObjects(graph, range, owl.oneOf)
        fillPossibleValuesFromList(enumerated, possibleValues)
      }
      if (!possibleValues.isEmpty) {
        println(s"populateFromOwlOneOf size ${possibleValues.size} ranges $ranges - $entryField")
        entryField.openChoice = false
      }
      //      entry.setPossibleValues(possibleValues ++ entry.possibleValues)
      /**
       * fill Possible Values into `possibleValues`
       * from given RDF List `enumerated`, which typically comes
       *  from existing triples with relevant rdf:type
       */
      def fillPossibleValuesFromList(
        enumerated: Iterable[Rdf#Node],
        possibleValues: mutable.ArrayBuffer[(Rdf#Node, Rdf#Node)]) =
        for (enum <- enumerated)
          foldNode(enum)(
            uri => {
              val list = nodeSeqToURISeq(rdfh.rdfListToSeq(Some(uri)))
              possibleValues.appendAll(
                list zip instanceLabels(list).map { s => makeLiteral(s, xsd.string) })
            },
            x => {
              println(s"fillPossibleValuesFromList bnode $x")
              val list = rdfh.rdfListToSeq(Some(x))
              possibleValues.appendAll(
                list zip instanceLabels(list).map { s => makeLiteral(s, xsd.string) })
            },
            x => { println(s"lit $x"); () })

      possibleValues.map { (couple: (Rdf#Node, Rdf#Node)) =>
        new ResourceWithLabel(couple._1, couple._2)
      }
    }

    def populateFromOwlUnion(): Map[ Rdf#Node, Seq[ResourceWithLabel[Rdf] ]] = {
      val classes = processOwlUnion()
      println( "populateFromOwlUnion: classes " + classes )
      val map = ( for( classe <- classes ) yield {
    	  classe -> tuples2ResourcesWithLabel( getInstancesAndLabels(classe) )
      } ) . toMap
      map
    }
    
    import org.w3.banana.binder._
    import org.w3.banana.syntax._

    /**
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
    
    /**
       * fill Possible Values into `possibleValues`
       * from given list `enumerated`, which typically comes
       *  from existing triples with relevant rdf:type
       *  TODO should use a yield, not an ArrayBuffer
       */
      def fillPossibleValues(enumerated: Iterable[Rdf#Node] ):
        Seq[(Rdf#Node, Rdf#Node)] = {
        val uriAndInstanceLabels = enumerated.toSeq.map {
          enum =>
            foldNode(enum)(
              uri => (uri, instanceLabel(uri, graph, "")),
              x => (x, instanceLabel(x, graph, "")),
              x => (x, ""))
        }
        val sortedInstanceLabels = uriAndInstanceLabels.toSeq.sortBy { e => e._2 }
          sortedInstanceLabels.map {
        	  c => (c._1, makeLiteral(c._2, xsd.string))
          }
//          List()
      }
      
    /** populate possible Values From Instances */
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
      println(s"  possibleValues.size ${entryField.property} ${possibleValues.size}" )
      val result = possibleValues.map {
        (couple: (Rdf#Node, Rdf#Node)) =>
        new ResourceWithLabel(couple._1, couple._2)
      }   
      result
    }

    def getInstancesAndLabels(rangeClass: Rdf#Node): Seq[(Rdf#Node, Rdf#Node)] = {
      if (rangeClass != owl.Thing) {
        val enumerated = getSubjects(graph, rdf.typ, rangeClass)
        val possibleValues2 = fillPossibleValues(enumerated)
        val subClasses = getSubjects(graph, rdfs.subClassOf, rangeClass)
        val r = for (subClass <- subClasses) yield {
          val subClassesValues = getSubjects(graph, rdf.typ, subClass)
          val possibleValues3 = fillPossibleValues(subClassesValues)
          possibleValues3
        }
        val rr = possibleValues2 ++ r.flatten.toSeq
        rr
      } else Seq()
    }
    
    /**
     * populate From configuration in TDB
     *  TODO ? merge given possibleValues with existing ones
     */
    def populateFromTDB(): Seq[ResourceWithLabel[Rdf]] = {
      if (!valuesFromFormGroup.isEmpty) {
        entryField.openChoice = false
      }
      valuesFromFormGroup.map { (couple: (Rdf#Node, Rdf#Node)) =>
        new ResourceWithLabel(couple._1, couple._2)
      }
    }

    /** this function does the job! */
    def setPossibleValues()
    // : Entry
    = {
      //      println("addPossibleValues " + entryField)
      val fieldType = entryField.type_
      //      println("addPossibleValues fieldType " + fieldType)
      val possibleValues = {
        if (isDefined(fieldType) ) {
          getPossibleValuesAsTuple(fieldType)
        } else {
          val resourcesWithLabelFromOwlUnion = populateFromOwlUnion()
          val resourcesWithLabel =
            populateFromTDB() ++
              populateFromInstances() ++
              populateFromOwlOneOf()
          val res = recordPossibleValues(resourcesWithLabel, resourcesWithLabelFromOwlUnion )
          res
        }
      }
//      entryField.setPossibleValues(possibleValues)
      possibleValues
    }

    /** record Possible Values */
    def recordPossibleValues(resourcesWithLabel: Seq[ResourceWithLabel[Rdf]],
                             classesAndLabels:  Map[Rdf#Node, Seq[ ResourceWithLabel[Rdf]]] ):
                             Seq[(Rdf#Node, Rdf#Node)] = {
      val fieldType = entryField.type_
      if (classesAndLabels isEmpty)
        addPossibleValues(fieldType, resourcesWithLabel)
      else {
        for( (classe, rlabels ) <- classesAndLabels ) {
        	addPossibleValues(classe, rlabels)
        }
        val r = classesAndLabels.values.flatMap( identity ) .toSeq
        resourcesWithLabel2Tuples(r)
      }
    }
    
    // ==== body of function addPossibleValues ====

//    entryField match {
//      case entryField: ResourceEntry => setPossibleValues
//      case entryField: BlankNodeEntry => setPossibleValues
//      case entryField: LiteralEntry => entryField
//    }
    setPossibleValues
  }

  // ========

  import sparqlGraph.sparqlEngineSyntax._

  /** @return list of VALUE & LABEL */
  def possibleValuesFromFormGroup(formGroup: Rdf#URI,
    graph1: Rdf#Graph)
    (implicit graph: Rdf#Graph)
    : Seq[(Rdf#Node, Rdf#Node)] = {
    val q = s"""
              prefix form: <http://deductions-software.com/ontologies/forms.owl.ttl#>
              prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
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
        //        info(s""" populateFromTDB iter ${row}""")
        (row("VALUE").get.as[Rdf#Node].get,
          row("LABEL").get.as[Rdf#Node].get)
    }
    val possibleValues = res.to[List]
    info(s""" populateFromTDB  size ${possibleValues.size}
             ${possibleValues.mkString("\n")}""")
    possibleValues
  }
  
}
