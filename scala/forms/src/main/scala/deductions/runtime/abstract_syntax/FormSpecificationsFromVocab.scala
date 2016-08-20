/* copyright the Déductions Project
under GNU Lesser General Public License
http://www.gnu.org/licenses/lgpl.html
$Id$
 */
package deductions.runtime.abstract_syntax

import java.io.File
import java.io.FileOutputStream
import java.io.FileReader

import scala.language.existentials
import scala.language.postfixOps

import org.apache.log4j.Logger
import org.w3.banana.RDF

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.utils.RDFPrefixes

/** input file for vocabulary; output in new file with ".formspec.ttl" suffix */
object FormSpecificationsFromVocabApp extends RDFStoreLocalJena1Provider
    with App
    with FormSpecificationsFromVocab[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  val logger: Logger = Logger.getRootLogger()
  if (args.size == 0) {
    println("Usage: input file for vocabulary; output in new file with '.formspec.ttl' suffix")
    System.exit(-1)
  }
  makeFormSpecificationsFromVocabFile(new File(args(0)))
}

/**
 * Create squeleton form specifications from an RDFS/OWL vocabulary;
 * the resulting RDF file can then be manually updated
 * (leverages abstract Form Syntax)
 *
 * NON transactional
 */
trait FormSpecificationsFromVocab[Rdf <: RDF, DATASET]
    extends UnfilledFormFactory[Rdf, DATASET]
    with RDFPrefixes[Rdf] {

  import ops._

  /** generate Form Specifications (skeletons to be hand edited) from an RDFS/OWL vocabulary */
  def makeFormSpecificationsFromVocabFile(vocabFile: File): Unit = {
    val reader = new FileReader(vocabFile)
    turtleReader.read(reader, "").map {
      graph =>
        val formSpecifications = makeFormSpecificationsFromVocab(graph)
        val formspecFile = new File(vocabFile.getAbsolutePath + ".formspec.ttl")
        val os = new FileOutputStream(formspecFile)
        turtleWriter.write(formSpecifications, os, "")
        println(s"Written: $formspecFile")
    }
  }

  /** generate Form Specifications (skeletons to be hand edited) from an RDFS/OWL vocabulary */
  def makeFormSpecificationsFromVocab(vocabGraph: Rdf#Graph): Rdf#Graph = {
    println(s"makeFormSpecificationsFromVocab: vocabGraph size ${vocabGraph.size}");
    implicit val graph: Rdf#Graph = vocabGraph
    val graphs = for (
      classTriple <- find(vocabGraph, ANY, rdf.typ, owl.Class);
      _ = println(s"makeFormSpecificationsFromVocab: class Triple $classTriple");
      classe = classTriple.subject;
      formSyntax = createFormFromClass(makeURI(classe));
      _ = println(s"makeFormSpecificationsFromVocab: formSyntax $formSyntax")
    ) yield {
      union(Seq(
        makeFormSpecificationFromFormSyntax(formSyntax),
        makeFieldSpecifications(formSyntax, vocabGraph)))
    }

    union(graphs.toSeq)
  }

  /** make Form Specification From Form Syntax */
  private def makeFormSpecificationFromFormSyntax(
    formSyntax: FormModule[Rdf#Node, Rdf#URI]#FormSyntax): Rdf#Graph = {
    val classs = formSyntax.classs
    val fields = formSyntax.fields
    val properties = (for (field <- fields) yield {
      field.property
    }).toList
    val classDomain = formvoc("classDomain")
    val showProperties = formvoc("showProperties")
    val form_URI = formURI(formSyntax)
    println("formURI " + form_URI + ", classs " + classs)
    val formGraph = (form_URI
      -- classDomain ->- classs
      -- showProperties ->- properties).graph
    formGraph
  }

  def formvoc(id: String) = prefixesMap2("form")(id)
  def formURI(formSyntax: FormModule[Rdf#Node, Rdf#URI]#FormSyntax) = URI(fromUri(formSyntax.classs) + "-formFromClass")

  /**
   * create form:DBPediaLookup field Specifications,
   * if it is an object property,
   * and there is no rdfs:range,
   * or if the rdfs:range is something prefixed by dbo: <http://dbpedia.org/ontology/> .
   *
   * like this:
   *
   * forms:topic_interest
   *  	:fieldAppliesToForm forms:personForm ;
   *  	:fieldAppliesToProperty foaf:topic_interest ;
   *  	:widgetClass form:DBPediaLookup ;
   *    :fieldClass foaf:Person .
   */
  private def makeFieldSpecifications(formSyntax: FormModule[Rdf#Node, Rdf#URI]#FormSyntax,
                              vocabGraph: Rdf#Graph): Rdf#Graph = {
    val graphs = for (
      field <- formSyntax.fields;
//      _ = println(s"field $field")
      if( field match {
        case re: ResourceEntry => true
        case _ => false
      });
      property = field.property;
      ranges = objectsQuery(property, rdfs.range)(vocabGraph)
      if (ranges.isEmpty ||
        ranges.exists { range => range.toString().startsWith(fromUri(prefixesMap("dbo"))) })
//      _ = println(s"ranges $ranges")
      if( ! List( rdfs.label, rdfs.comment, rdf.typ ) . contains(property) )
    ) yield {
      val form_URI = formURI(formSyntax)
      val fieldURI = URI(property.toString() + "-fieldSpecification")
      (fieldURI
        -- formvoc("fieldAppliesToForm") ->- form_URI
        -- formvoc("fieldAppliesToProperty") ->- property
        -- formvoc("widgetClass") ->- formvoc("DBPediaLookup")
        -- formvoc("fieldClass") ->- ranges.headOption).graph
    }
    union(graphs)
  }
}
