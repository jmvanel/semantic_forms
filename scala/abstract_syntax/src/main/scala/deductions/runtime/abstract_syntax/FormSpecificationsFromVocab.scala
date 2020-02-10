/* copyright the Déductions Project
under GNU Lesser General Public License
http://www.gnu.org/licenses/lgpl.html
$Id$
 */
package deductions.runtime.abstract_syntax

import java.io.{File, FileOutputStream, StringReader}
import java.net.URL

import deductions.runtime.core.FormModule
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.core.HTTPrequest

import org.w3.banana.RDF

import scala.io.Source
import scala.language.{existentials, postfixOps}
import scala.util.Try

/** input file for vocabulary; output in new file with ".formspec.ttl" suffix */


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
  def makeFormSpecificationsFromVocabFile(
  res: Try[Rdf#Graph]    ,
      vocabFile: File
  ): Unit = {
    println(s"makeFormSpecificationsFromVocabFile: $res")
    res.map {
      graph =>
        println(s"graph size ${graph.size}")
        val formSpecifications = makeFormSpecificationsFromVocab(graph)
        val formspecFile = new File(vocabFile.getAbsolutePath + ".formspec.ttl")
        val os = new FileOutputStream(formspecFile)
        turtleWriter.write(formSpecifications, os, "")
        println(s"Written: $formspecFile")
    }
  }

  def readFile(vocabFile: File): Try[Rdf#Graph]  = {
		val bufferedSource = Source.fromFile(vocabFile)
    val content = bufferedSource.getLines().mkString("\n")
    bufferedSource.close()
    val reader = new StringReader(content)
    val res = turtleReader.read(reader, "")
    println(s"turtle Reader: ${res}")
    res
  }

  def readTurtleTerm(term: String): Try[Rdf#Graph] = {
	  println(s"readTurtleTerm ${expandOrUnchanged(term)}")
    rdfLoader.load(
      new URL(expandOrUnchanged(term)))
  }
  
  private def readTurtleTermTurtle(term: String): Try[Rdf#Graph] = {
    val bufferedSource = Source.fromURL(
      expandOrUnchanged(term))
    // PASTED
    val content = bufferedSource.getLines().mkString("\n")
    bufferedSource.close()
    val reader = new StringReader(content)
    val res = turtleReader.read(reader, "")
    println(s"turtle Reader: ${res}")
    res
  }
    
  /** generate Form Specifications (skeletons to be hand edited) from an RDFS/OWL vocabulary */
  def makeFormSpecificationsFromVocab(vocabGraph: Rdf#Graph): Rdf#Graph = {
    println(s"makeFormSpecificationsFromVocab: vocabGraph size ${vocabGraph.size}");
    implicit val graph: Rdf#Graph = vocabGraph
    val graphs = for (
      classTriple <- find(vocabGraph, ANY, rdf.typ, owl.Class);
      _ = println(s"makeFormSpecificationsFromVocab: class Triple $classTriple");
      classe = classTriple.subject;
      formSyntax = createFormFromClass( makeURI(classe), request=HTTPrequest(acceptLanguages=Seq("en")) )(graph);
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
      -- classDomain ->- classs.head // TODO
      -- showProperties ->- properties).graph
    formGraph
  }

  def formvoc(id: String) = prefixesMap2("form")(id)
  def formURI(formSyntax: FormModule[Rdf#Node, Rdf#URI]#FormSyntax) =
    URI(fromUri(
        // TODO multi-type:
        uriNodeToURI(formSyntax.classs.head)) + "-formFromClass")

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
