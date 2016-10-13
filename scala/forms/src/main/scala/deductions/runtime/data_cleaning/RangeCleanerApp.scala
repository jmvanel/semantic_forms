package deductions.runtime.data_cleaning

import java.io.File

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.services.SPARQLHelpers
import org.w3.banana.OWLPrefix
import org.w3.banana.RDFSPrefix
import org.w3.banana.RDF

/**
 * Removes the less specific Datatype.
 * eg for ranges List(http://www.w3.org/2001/XMLSchema#double, http://www.w3.org/2001/XMLSchema#long)
 * removes long, etc for string, ...
 *
 * Argument
 * - file
 *
 * Output:
 * modified data file in /tmp (same name as input)
 */
object RangeCleanerApp extends App
    with ImplementationSettings.RDFCache
    with RDFCacheAlgo[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with RangeCleaner[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  import ops._

  override val databaseLocation = "/tmp/TDB" // TODO multi-platform temporary directory
  override val deleteDatabaseLocation = true
  override val useTextQuery = false

  println(s"databaseLocation $databaseLocation")
  rangeCleanerApp()

  def rangeCleanerApp() = {
    possiblyDeleteDatabaseLocation()
    val args2 = args.map { new File(_).getCanonicalPath }
    val classURI = ops.URI(args(0))
    println(s"classURI $classURI")
    val files = loadFilesFromArgs(args2, from = 0)
    println(removeRangeDuplicates())
    outputModifiedTurtle(files(0), suffix=".rangeCleaner.ttl")
  }
}

trait RangeCleaner[Rdf <: RDF, DATASET]
    extends DuplicateCleaner[Rdf, DATASET]
{
  import ops._
  import rdfStore.graphStoreSyntax._
  import rdfStore.transactorSyntax._

  val rangePriorities = List(
    (xsd.long, xsd.string),
    (xsd.long, xsd.integer),
    (xsd.double, xsd.long))

  def computeRangesToRemove(ranges: List[Rdf#Node]): List[Rdf#Node] = {
	  if( ranges.size > 1 )
	    println( s">>>> computeRangesToRemove ranges $ranges" )
    if (ranges.size > 1) {
      val rm = for ( range <- ranges;
         (a, b)  <- rangePriorities;
//         rr = println( s">>>> a $a b $b" )
         if( range == b && ranges.contains(a) )
         ) yield b
      rm
    } else List()
  }

  def removeRangeDuplicates(graphURI: String=fromUri(globalNamedGraph)) = {
    rdfStore.rw(dataset, {
      val datatypePropertyTriples = find(allNamedGraph, ANY, rdf.typ, owl.DatatypeProperty).toList
      val datatypeProperties = datatypePropertyTriples. map { tr => tr.subject }
      for (datatypeProperty <- datatypeProperties) {
//    	  if( datatypeProperty.toString().contains( "Concours/epreuves/coefficient" ) ) {
//    		 println(s"datatypeProperty $datatypeProperty")
//    	  }
        val ranges = find(allNamedGraph, datatypeProperty, rdfs.range, ANY).map { tr => tr.objectt }.toList
        val rangesToRemove = computeRangesToRemove(ranges)
        val triples = for (r <- rangesToRemove) yield {
          Triple(datatypeProperty, rdfs.range, r)
        }
        if( ! rangesToRemove.isEmpty )
        	println( s"datatypeProperty $datatypeProperty rangesToRemove $rangesToRemove graphURI $graphURI \n" )
        rdfStore.removeTriples(dataset, URI(graphURI), triples)
      }
    })
  }
}