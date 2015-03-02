package deductions.runtime.abstract_syntax

import org.w3.banana.RDF
import org.w3.banana.OWLPrefix
import org.w3.banana.FOAFPrefix
import deductions.runtime.utils.RDFHelpers
import deductions.runtime.jena.RDFStoreObject
import scala.collection._
import org.w3.banana.SparqlGraphModule
import org.w3.banana.SparqlEngine
import scala.util.Try
import org.w3.banana.SparqlOps
import org.apache.log4j.Logger

/**
 * populate Fields in form by inferring possible values from given rdfs:range's URI,
 *  through owl:oneOf and know instances
 */
trait RangeInference[Rdf <: RDF] extends InstanceLabelsInference[Rdf] {
  self: FormSyntaxFactory[Rdf] =>

  implicit val sparqlGraph: SparqlEngine[Rdf, Try, Rdf#Graph]
  implicit val sparqlOps: SparqlOps[Rdf]

  import ops._

  def addPossibleValues(entryField: Entry, ranges: Set[Rdf#Node],
    formGroup: Rdf#URI): Entry = {
    val owl = OWLPrefix[Rdf]
    val gr = graph
    val rdfh = new RDFHelpers[Rdf] { val graph = gr }

    /**
     * modify entry to populate possibleValues,
     * by taking ?LIST from triples:
     * ?RANGE owl:oneOf ?LIST
     */
    def populateFromOwlOneOf(entry: Entry): Entry = {
      val possibleValues = mutable.ArrayBuffer[(Rdf#Node, String)]()
      for (range <- ranges) {
        val enumerated = ops.getObjects(graph, range, owl.oneOf)
        fillPossibleValuesFromList(enumerated, possibleValues)
      }
      entry.openChoice = false
      entry.setPossibleValues(possibleValues ++ entry.possibleValues)
    }

    /**
     * fill Possible Values From given List, which typically comes
     *  from existing triples with relevant rdf:type
     */
    def fillPossibleValuesFromList(enumerated: Iterable[Rdf#Node],
      possibleValues: mutable.ArrayBuffer[(Rdf#Node, String)]) =
      for (enum <- enumerated)
        ops.foldNode(enum)(
          uri => {
            val list = rdfh.nodeSeqToURISeq(rdfh.rdfListToSeq(Some(uri)))
            possibleValues.appendAll(
              list zip instanceLabels(list)
            )
          },
          x => {
            println(s"bnode $x")
            //            val list = rdfh.nodeSeqToURISeq(rdfh.rdfListToSeq(Some(x)))
            val list = rdfh.rdfListToSeq(Some(x))
            possibleValues.appendAll(
              list zip instanceLabels(list)
            )
          },
          x => { println(s"lit $x"); () })

    /** modify entry to populate possible Values From Instances */
    def populateFromInstances(entry: Entry): Entry = {
      val possibleValues = mutable.ArrayBuffer[(Rdf#Node, String)]()
      // debug      //      val personURI = ops.URI("http://xmlns.com/foaf/0.1/Person")
      //      if (ranges.contains(personURI)) {
      //        println(s"populateFromInstances: entry $entry")
      //        val triples = ops.find(graph, ANY, rdf.typ, personURI)
      //        println(s"populateFromInstances: triples size ${triples.size}")
      //        for (t <- triples) println(t._1)
      //      }
      for (range <- ranges) {
        // TODO also take in account subClassOf inference
        // TODO limit number of possible values; later implement Comet on demand access to possible Values
        val enumerated = ops.getSubjects(graph, rdf.typ, range)
        // debug        //        if (range == personURI) {
        //          println(s"populateFromInstances: enumerated ${enumerated.mkString("; ")}")
        //        }
        fillPossibleValues(enumerated, possibleValues)
        // debug
        //        if (range == personURI) println(s"possibleValues $possibleValues")
      }
      entry.setPossibleValues(possibleValues ++ entry.possibleValues)
    }

    def fillPossibleValues(enumerated: Iterable[Rdf#Node],
      possibleValues: mutable.ArrayBuffer[(Rdf#Node, String)]) =
      for (enum <- enumerated)
        ops.foldNode(enum)(
          uri => {
            possibleValues.append(
              (uri, instanceLabel(uri))
            )
          },
          x => (), x => ())

    /**
     * populate From configuration in TDB
     *  TODO move this SPARQL query outside of a loop on form fields
     */
    def populateFromTDB(entry: Entry): Entry = {
      if (formGroup != nullURI) {
        val q = s"""
              prefix form: <http://deductions-software.com/ontologies/forms.owl.ttl#>
              prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
              SELECT ?VALUE ?LABEL
              WHERE {
              GRAPH ?GR {
              <${formGroup}> form:labelsForFormGroup ?LABELS .
              ?LABELS form:labelsForValues ?BN .
              ?BN form:value ?VALUE ; rdfs:label ?LABEL . 
              } }
              """
        info(s"populateFromTDB $q")
        val query = sparqlOps.parseSelect(q, Seq()).get
        val solutions = sparqlGraph.executeSelect(graph, query,
          scala.collection.immutable.Map()).get
        val vars = Seq("VALUE", "LABEL")
        val res = solutions.toIterable map {
          row =>
            (row("VALUE").get.as[Rdf#Node].get,
              row("LABEL").get.as[String].get)
        }
        val possibleValues = res.toSeq
        info(s"populateFromTDB ${possibleValues.mkString("\n")}")

        entry.openChoice = false
        entry.setPossibleValues(possibleValues ++ entry.possibleValues)
      } else entry
    }

    // ==== body of function addPossibleValues ====

    populateFromTDB(
      populateFromInstances(
        populateFromOwlOneOf(entryField)))
  }

  def info(s: String) = Logger.getRootLogger().info(s)
}