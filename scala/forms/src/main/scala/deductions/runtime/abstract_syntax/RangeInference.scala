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

  def addPossibleValues(
    entryField: Entry,
    ranges: Set[Rdf#Node],
    valuesFromFormGroup: Seq[(Rdf#Node, Rdf#Node)])
  (implicit graph: Rdf#Graph)
  : Entry = {

    val owl = OWLPrefix[Rdf]
    val rdfh: RDFHelpers[Rdf] = this

    /**
     * modify entry to populate possibleValues,
     * by taking ?LIST from triples:
     * ?RANGE owl:oneOf ?LIST
     */
    def populateFromOwlOneOf(entry: Entry): Seq[ResourceWithLabel[Rdf]] = {
      val possibleValues = mutable.ArrayBuffer[(Rdf#Node, Rdf#Node)]()
      for (range <- ranges) {
        val enumerated = getObjects(graph, range, owl.oneOf)
        fillPossibleValuesFromList(enumerated, possibleValues)
      }
      if (!possibleValues.isEmpty) {
        println(s"populateFromOwlOneOf size ${possibleValues.size} ranges $ranges - $entry")
        entry.openChoice = false
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

    def populateFromOwlUnion(entry: Entry): Map[ Rdf#URI, Seq[ResourceWithLabel[Rdf] ]] = {
      val classes = processOwlUnion(entry)
      Map() // TODO <<<<<<<<<
    }
    
    def processOwlUnion(entry: Entry): Seq[Rdf#URI] = {
    	Seq() // TODO <<<<<<<<<
    }
    
    /** modify entry to populate possible Values From Instances */
    def populateFromInstances(entry: Entry): Seq[ResourceWithLabel[Rdf]] = {
      val possibleValues = mutable.ArrayBuffer[(Rdf#Node, Rdf#Node)]()
      //      if (entry.label == "knows") println("knows")
      time(s"populateFromInstances ${entry.label}",
        for (range <- ranges) {
          // TODO also take in account subClassOf inference
          // TODO limit number of possible values; later implement Comet on demand access to possible Values
          if (range != owl.Thing) {
            val enumerated = getSubjects(graph, rdf.typ, range)
            fillPossibleValues(enumerated, possibleValues)

            val subClasses = getSubjects(graph, rdfs.subClassOf, range)
            for (subClass <- subClasses) {
              val subClassesValues = getSubjects(graph, rdf.typ, subClass)
              fillPossibleValues(subClassesValues, possibleValues)
            }
          }
        })
      /**
       * fill Possible Values into `possibleValues`
       * from given list `enumerated`, which typically comes
       *  from existing triples with relevant rdf:type
       */
      def fillPossibleValues(enumerated: Iterable[Rdf#Node],
        possibleValues: mutable.ArrayBuffer[(Rdf#Node, Rdf#Node)]): Unit = {
        val r = enumerated.toSeq.map {
          enum =>
            foldNode(enum)(
              uri => (uri, instanceLabel(uri, graph, "")),
              x => (x, instanceLabel(x, graph, "")),
              x => (x, ""))
        }
        val sortedInstanceLabels = r.toSeq.sortBy { e => e._2 }
        // println(s"sortedInstanceLabels ${sortedInstanceLabels.takeRight(5).mkString(", ")}")
        possibleValues ++= (sortedInstanceLabels.map {
          c => (c._1, makeLiteral(c._2, xsd.string))
        })
      }
      println("  possibleValues.size " + possibleValues.size)
      possibleValues.map { (couple: (Rdf#Node, Rdf#Node)) =>
        new ResourceWithLabel(couple._1, couple._2)
      }
      //      entry.setPossibleValues(possibleValues ++ entry.possibleValues)
    }

    /**
     * populate From configuration in TDB
     *  TODO ? merge given possibleValues with existing ones
     */
    def populateFromTDB(entry: Entry): Seq[ResourceWithLabel[Rdf]] = {
      if (!valuesFromFormGroup.isEmpty) {
        entry.openChoice = false
        //        entry.setPossibleValues(valuesFromFormGroup ++ entry.possibleValues)
      }
      //      else entry
      valuesFromFormGroup.map { (couple: (Rdf#Node, Rdf#Node)) =>
        new ResourceWithLabel(couple._1, couple._2)
      }
    }

    /** this function does the job! */
    def setPossibleValues(): Entry = {
      //      println("addPossibleValues " + entryField)
      val fieldType = entryField.type_
      //      println("addPossibleValues fieldType " + fieldType)
      val possibleValues = {
        if (isDefined(fieldType)) {
          getPossibleValuesAsTuple(fieldType)
        } else {
          val resourcesWithLabelFromOwlUnion = populateFromOwlUnion(entryField)
          val resourcesWithLabel =
            populateFromTDB(entryField) ++
              populateFromInstances(entryField) ++
              populateFromOwlOneOf(entryField)
//              resourcesWithLabelFromOwlUnion._1
          val res = recordPossibleValues(resourcesWithLabel, resourcesWithLabelFromOwlUnion )
          //          println(s"addPossibleValues(fieldType, $resourcesWithLabel)")
          res
        }
      }
      entryField.setPossibleValues(possibleValues)
    }

    /** record Possible Values */
    def recordPossibleValues(resourcesWithLabel: Seq[ResourceWithLabel[Rdf]],
                             classesAndLabels:  Map[Rdf#URI, Seq[ ResourceWithLabel[Rdf]]] ):
                             Seq[(Rdf#Node, Rdf#Node)] = {
      val fieldType = entryField.type_
      if (classesAndLabels isEmpty)
        addPossibleValues(fieldType, resourcesWithLabel)
      else {
        for( (classe, rlabels ) <- classesAndLabels ) {
        	addPossibleValues(classe, rlabels)
        }
        Seq() // TODO <<<<<<<<<<<<<<<<<<<<<
      }
    }
    
    // ==== body of function addPossibleValues ====

    entryField match {
      case entryField: ResourceEntry => setPossibleValues
      case entryField: BlankNodeEntry => setPossibleValues
      case entryField: LiteralEntry => entryField
    }
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
