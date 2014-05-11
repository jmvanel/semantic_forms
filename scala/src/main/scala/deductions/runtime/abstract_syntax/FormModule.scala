/* copyright the Déductions Project
under GNU Lesser General Public License
http://www.gnu.org/licenses/lgpl.html
$Id$
 */
package deductions.runtime.abstract_syntax

import org.w3.banana._
import org.w3.banana.diesel._
import org.w3.banana.syntax._
import scala.collection.mutable

trait FormModule[URI] {
  /**
   * abstract_syntax for a semantic form:
   *  - generated from a list of URI's for properties, and a triple store
   *  - used in conjunction with N3Form(Swing), HTML5 forms and Banana-RDF
   */
  case class FormSyntax[URI](
    val subject: URI,
    val fields: Seq[Entry]) {
    override def toString() : String = {
      "FormSyntax:\n\t" + 
    	fields.mkString("\n")
    }
  }
  
  val nullURI : URI
  class Entry(val label: String, val comment: String // , val value:Any
  ) {
    	override def toString() : String = {
    		label + " " + comment
    	}
  }
  case class ResourceEntry( l: String,  c: String,
    property: ObjectProperty, validator: ResourceValidator,
    value: URI=nullURI) extends Entry(l, c) {
    	override def toString() : String = {
    		super.toString + ", " + value
    	}
  }
  case class LiteralEntry(l: String, c: String,
    property: DatatypeProperty, validator: DatatypeValidator,
    value: String = "") extends Entry(l, c) {
  	override def toString() : String = {
  		super.toString  + ", " + value
  	}
  }

  type DatatypeProperty = URI
  type ObjectProperty = URI

  case class ResourceValidator(typ:URI)
  case class DatatypeValidator(typ:URI)
}
