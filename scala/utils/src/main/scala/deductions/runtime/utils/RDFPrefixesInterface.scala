package deductions.runtime.utils



import org.apache.jena.riot.RiotException
import org.w3.banana.jena.{Jena, JenaModule}

/**
  * Created by tristan on 13/07/17.
  */
trait RDFPrefixesInterface {






  object ImplementationSettings {
    // pave the way for migration to Jena 3 ( or BlazeGraph )
    type DATASET = org.apache.jena.query.Dataset
    type Rdf = Jena
    type RDFModule = JenaModule
    /** actually just RDF database location; TODO rename RDFDatabase */

    type RDFReadException = RiotException
  }

}
