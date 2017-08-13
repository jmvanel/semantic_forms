package deductions.runtime.utils

import org.w3.banana.RDF

case class DatabaseChanges[Rdf <: RDF](
		  triplesToAdd: Seq[Rdf#Triple] = Seq(),
		  triplesToRemove: Seq[Rdf#Triple] = Seq(),
		  typeChange: Boolean = false)
