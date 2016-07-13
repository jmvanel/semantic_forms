/* copyright the Déductions Project
under GNU Lesser General Public License
http://www.gnu.org/licenses/lgpl.html
$Id$
 */
package deductions.runtime.abstract_syntax

import scala.collection.mutable
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import scala.language.postfixOps
import scala.language.existentials

import org.apache.log4j.Logger
import org.apache.log4j.Level

import org.w3.banana.OWLPrefix
import org.w3.banana.PointedGraph
import org.w3.banana.RDF
import org.w3.banana.RDFDSL
import org.w3.banana.RDFOps
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFSPrefix
import org.w3.banana.XSDPrefix
import org.w3.banana.diesel._
import org.w3.banana.Prefix
import org.w3.banana.SparqlOps
import org.w3.banana.RDFOpsModule
import org.w3.banana.syntax._
import org.w3.banana.SparqlEngine
import org.w3.banana.LocalNameException

import deductions.runtime.utils.RDFHelpers
import deductions.runtime.utils.Timer
import deductions.runtime.services.Configuration
import deductions.runtime.utils.RDFPrefixes

/**
 * Create form specifications from an abstract Form Syntax;
 * the resulting RDF file can then be manually updated
 *
 * NON transactional
 */
trait FormSpecificationsFromVocab[Rdf <: RDF, DATASET]
    extends FormModule[Rdf#Node, Rdf#URI]
    with RDFHelpers[Rdf]
    with FieldsInference[Rdf, DATASET]
    with RangeInference[Rdf, DATASET]
    with PreferredLanguageLiteral[Rdf]
    with PossibleValues[Rdf]
    with FormConfigurationFactory[Rdf, DATASET]
    with RDFPrefixes[Rdf]
    with Timer {

  import ops._

  def makeFormSpecificationFromFormSyntax(formSyntax: FormModule[Rdf#Node, Rdf#URI]#FormSyntax): Rdf#Graph = {
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

  //	def makeFormSyntaxFromVocab( vocabGraph: Rdf#Graph ): Rdf#Graph = {  
  //  }
  //  def makeFormSyntaxFromClass( classe: Rdf#URI, graph: Rdf#Graph ): Rdf#Graph = {   
  //  }
}
