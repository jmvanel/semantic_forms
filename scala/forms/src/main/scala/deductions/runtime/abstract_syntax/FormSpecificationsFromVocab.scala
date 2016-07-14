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


object FormSpecificationsFromVocabApp extends RDFStoreLocalJena1Provider
    with App
    with FormSpecificationsFromVocab[
    ImplementationSettings.Rdf, ImplementationSettings.DATASET
] {
    val logger:Logger = Logger.getRootLogger()
    makeFormSpecificationsFromVocabFile( new File(args(0)) )
}

/**
 * Create form specifications from an RDFS/OWL vocabulary;
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
		println(s"makeFormSpecificationsFromVocab: vocabGraph ${vocabGraph.size}") ;
    implicit val graph: Rdf#Graph = vocabGraph
    val graphs = for (
      classeTr <- find(vocabGraph, ANY, rdf.typ, owl.Class);
      zz = println(s"makeFormSpecificationsFromVocab: classeTr $classeTr") ;
      classe = classeTr.subject;
      formSyntax = createFormFromClass(makeURI(classe)) ;
      zzz = println(s"makeFormSpecificationsFromVocab: formSyntax $formSyntax")
    ) yield makeFormSpecificationFromFormSyntax(formSyntax)

    union(graphs.toSeq)
  }

  private def makeFormSpecificationFromFormSyntax(formSyntax: FormModule[Rdf#Node, Rdf#URI]#FormSyntax): Rdf#Graph = {
		println(s"makeFormSpecificationFromFormSyntax: formSyntax $formSyntax")

    val classs = formSyntax.classs
    val fields = formSyntax.fields
    val properties = for (field <- fields) yield {
      field.property
    }
    val classDomain = prefixesMap2("form")("classDomain")
    val showProperties = prefixesMap2("form")("showProperties")
    val formURI = ops.withFragment(classs, "formFromClass")

    val formGraph = (formURI
      -- classDomain ->- classs
      -- showProperties ->- properties.toList).graph

    /* TODO also create form:DBPediaLookup Form Specifications, like this:
     * 
     * forms:topic_interest
     *  	:fieldAppliesToForm forms:personForm ;
     *  	:fieldAppliesToProperty foaf:topic_interest ;
     *  	:widgetClass form:DBPediaLookup .
     */
    formGraph
  }

}
