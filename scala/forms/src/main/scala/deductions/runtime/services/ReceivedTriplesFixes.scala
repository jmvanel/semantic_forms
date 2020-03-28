package deductions.runtime.services

import deductions.runtime.utils.DatabaseChanges
import org.w3.banana.RDF
import org.w3.banana.RDFOps

/** FIX triples with bad xsd:time literal : missing seconds part */
trait ReceivedTriplesFixes[Rdf <: RDF] {
  implicit val ops: RDFOps[Rdf]
  import ops._
  def fixReceivedTriples(databaseChanges: DatabaseChanges[Rdf]): DatabaseChanges[Rdf] = {
    val newTriples = for (triple <- databaseChanges.triplesToAdd) yield {
      val obj =
        foldNode(triple.objectt)(
          uri => uri,
          bn => bn,
          literal => {
            val Literal(lexicalForm, datatype, _) = literal
            if (datatype == xsd("time") &&
              lexicalForm.matches("^[0-9][0-9]:[0-9][0-9]"))
              Literal(lexicalForm + ":00", datatype)
            else literal
          })
      Triple(triple.subject, triple.predicate, obj)
    }
    DatabaseChanges(newTriples, databaseChanges.triplesToRemove, databaseChanges.typeChange)
  }
}