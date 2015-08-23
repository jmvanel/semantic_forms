package deductions.runtime.abstract_syntax

import org.w3.banana.RDF
import org.w3.banana.OWLPrefix
import org.w3.banana.FOAFPrefix
import deductions.runtime.utils.RDFHelpers
import scala.collection._
import org.w3.banana.SparqlGraphModule
import org.w3.banana.SparqlEngine
import scala.util.Try
import org.w3.banana.SparqlOps
import org.apache.log4j.Logger
import java.net.URL
import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.utils.Timer

/**
 * populate Fields in form by inferring possible values from given rdfs:range's URI,
 *  through owl:oneOf and know instances
 */
trait RangeInference[Rdf <: RDF] extends InstanceLabelsInference2[Rdf]
    with Timer {
  self: FormSyntaxFactory[Rdf] =>

  implicit val sparqlGraph: SparqlEngine[Rdf, Try, Rdf#Graph]
  implicit val sparqlOps: SparqlOps[Rdf]
  implicit private val graphImplicit = graph

  //  import FormSyntaxFactory._
  import ops._
  import sparqlOps._
  import sparqlGraph._
  import sparqlGraph.sparqlEngineSyntax._

  def addPossibleValues(
    entryField: Entry,
    ranges: Set[Rdf#Node],
    valuesFromFormGroup: Seq[(Rdf#Node, Rdf#Node)]): Entry = {
    val owl = OWLPrefix[Rdf]
    val rdfh = {
      val gr = graph
      new RDFHelpers[Rdf] { val graph = gr }
    }

    /**
     * modify entry to populate possibleValues,
     * by taking ?LIST from triples:
     * ?RANGE owl:oneOf ?LIST
     */
    def populateFromOwlOneOf(entry: Entry): Entry = {
      val possibleValues = mutable.ArrayBuffer[(Rdf#Node, Rdf#Node)]()
      for (range <- ranges) {
        val enumerated = getObjects(graph, range, owl.oneOf)
        fillPossibleValuesFromList(enumerated, possibleValues)
      }
      if (!possibleValues.isEmpty) {
        println(s"populateFromOwlOneOf size ${possibleValues.size} ranges $ranges - $entry")
        entry.openChoice = false
      }
      entry.setPossibleValues(possibleValues ++ entry.possibleValues)
    }

    /**
     * fill Possible Values From given List, which typically comes
     *  from existing triples with relevant rdf:type
     */
    def fillPossibleValuesFromList(
      enumerated: Iterable[Rdf#Node],
      possibleValues: mutable.ArrayBuffer[(Rdf#Node, Rdf#Node)]) =
      for (enum <- enumerated)
        foldNode(enum)(
          uri => {
            val list = rdfh.nodeSeqToURISeq(rdfh.rdfListToSeq(Some(uri)))
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

    /** modify entry to populate possible Values From Instances */
    def populateFromInstances(entry: Entry): Entry = {
      val possibleValues = mutable.ArrayBuffer[(Rdf#Node, Rdf#Node)]()
      // debug      
      //      val personURI = URI("http://xmlns.com/foaf/0.1/Person")
      //      if (ranges.contains(personURI)) {
      //        println(s"populateFromInstances: entry $entry")
      //        val triples = find(graph, ANY, rdf.typ, personURI)
      //        println(s"populateFromInstances: triples size ${triples.size}")
      //        for (t <- triples) println(t._1)
      //      }
      if (entry.label == "knows")
        println("knows")
      time(s"populateFromInstances ${entry.label}",
        for (range <- ranges) {
          // TODO also take in account subClassOf inference
          // TODO limit number of possible values; later implement Comet on demand access to possible Values
          if (range != owl.Thing) {
            val enumerated = getSubjects(graph, rdf.typ, range)
            // debug        
            //        if (range == personURI)
            //          println(s"populateFromInstances: enumerated ${enumerated.mkString("; ")}")

            fillPossibleValues(enumerated, possibleValues)

            val subClasses = getSubjects(graph, rdfs.subClassOf, range)
            for (subClass <- subClasses) {
              val subClassesValues = getSubjects(graph, rdf.typ, subClass)
              fillPossibleValues(subClassesValues, possibleValues)
            }
            // debug  if (range == personURI) println(s"possibleValues $possibleValues")
          }
        }
      )
      entry.setPossibleValues(possibleValues ++ entry.possibleValues)
    }

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

    /**
     * populate From configuration in TDB
     *  TODO ? merge given possibleValues with existing ones
     */
    def populateFromTDB(entry: Entry): Entry = {
      if (!valuesFromFormGroup.isEmpty) {
        entry.openChoice = false
        entry.setPossibleValues(valuesFromFormGroup ++ entry.possibleValues)
      } else
        entry
    }

    // ==== body of function addPossibleValues ====

    populateFromTDB(
      populateFromInstances(
        populateFromOwlOneOf(entryField)))
  }

  def info(s: String) = Logger.getRootLogger().info(s)

  import sparqlGraph.sparqlEngineSyntax._

  /** @return list of VALUE & LABEL */
  def possibleValuesFromFormGroup(formGroup: Rdf#URI,
    graph1: Rdf#Graph): Seq[(Rdf#Node, Rdf#Node)] = {
    val q = s"""
              prefix form: <http://deductions-software.com/ontologies/forms.owl.ttl#>
              prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
              SELECT ?VALUE ?LABEL
              WHERE {
                <${formGroup}> form:labelsForFormGroup ?LABELS .
                ?LABELS form:labelsForValues ?BN .
                ?BN form:value ?VALUE ; rdfs:label ?LABEL . 
              }
              """
    //    info(s"populateFromTDB $q")

    val query = parseSelect(q, Seq()).get
    val solutions: Rdf#Solutions = graph.executeSelect(query).get
    //    import ops._

    val res = solutions.iterator() map {
      row =>
        //        info(s""" populateFromTDB iter ${row}""")
        (row("VALUE").get.as[Rdf#Node].get,
          row("LABEL").get.as[Rdf#Node].get)
    }
    val possibleValues = res.to[List] // Rdf#Node,String]]
    info(s""" populateFromTDB  size ${possibleValues.size}
             ${possibleValues.mkString("\n")}""")
    possibleValues
  }

  //// DEBUG (unused) ////
  /** TODO put it in util class or use SPARQLHelper in project sparql_client */
  def runSparqlSelect(
    queryString: String, variables: Seq[String],
    graph: Rdf#Graph): List[Seq[Rdf#URI]] = {
    val query = parseSelect(queryString).get
    val answers: Rdf#Solutions = graph.executeSelect(query).get
    val results: Iterator[Seq[Rdf#URI]] = answers.toIterable map {
      row =>
        for (variable <- variables) yield row(variable).get.as[Rdf#URI].get
    }
    results.to[List]
  }

  def dumpGraph() = {
    val selectAll = """
              # CONSTRUCT { ?S ?P ?O . }
              SELECT ?S ?P ?O
              WHERE {
                GRAPH ?GR {
                ?S ?P ?O .
                } }
            """
    val res2 = runSparqlSelect(selectAll, Seq("S", "P", "O"), graph)
    info(s""" populateFromTDB selectAll size ${res2.size}
             ${res2.mkString("\n")}""")
  }
}