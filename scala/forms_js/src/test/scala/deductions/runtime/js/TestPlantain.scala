package deductions.runtime.js

import org.w3.banana.RDF
import org.w3.banana.plantain.Plantain
import org.w3.banana.RDFOps
import org.w3.banana.plantain.PlantainOps

import org.scalajs.dom
import dom.document

trait TestPlantain[ Rdf <: RDF] {
  val ops: RDFOps[ Rdf ]
  import ops._
  
  lazy val tr = ops.Triple( URI("s") , URI("p"), Literal("o") )
}

object App1 extends App with TestPlantain[Plantain]{
  val ops = PlantainOps
  
  println( tr )

}
