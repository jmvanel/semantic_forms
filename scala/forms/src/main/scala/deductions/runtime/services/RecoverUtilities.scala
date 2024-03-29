package deductions.runtime.services

import org.w3.banana.RDF
import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.sparql_cache.SPARQLHelpers
import deductions.runtime.core.HTTPrequest

trait RecoverUtilities[Rdf <: RDF, DATASET]
extends  RDFStoreLocalProvider[Rdf, DATASET]
with SPARQLHelpers[Rdf, DATASET]
{

  private val runtime = Runtime.getRuntime

  def recoverFromOutOfMemoryErrorGeneric[T](
     sourceCode: => T,
     error: Throwable => T ): T = {

    val freeMemory = runtime.freeMemory()
    if( freeMemory < 1024 * 1024 * 10) {
      wrapInTransaction{ syncTDB() }
      System.gc()
        val freeMemoryAfter = Runtime.getRuntime().freeMemory()
        logger.info(s"recoverFromOutOfMemoryErrorGeneric: JVM Free memory after syncTDB & gc(): $freeMemoryAfter")
      error(new OutOfMemoryError(s"Free Memory was $freeMemory < 10 mb, retry later (now $freeMemoryAfter)"))
   } else
    try {
      sourceCode
    } catch {
      case t: Throwable =>
        t.printStackTrace()
        printMemory
        error(t)
    }
  }

  def formatMemory(): String = {
    val mb = 1024 * 1024
    "\n##### Heap utilization statistics [MB] #####\n" +
    "Used Memory:" + (runtime.totalMemory() - runtime.freeMemory()).toFloat / mb +
    "\nFree Memory:" + runtime.freeMemory().toFloat / mb +
    "\nTotal Memory:" + runtime.totalMemory().toFloat / mb +
    "\nMax Memory:" + runtime.maxMemory().toFloat / mb + "\n"
  }

  def printMemory = logger.info( formatMemory() )

  /** unused yet */
  def errorStringFromThrowable(
    t:               Throwable,
    specificMessage: String    = "ERROR",
    request: HTTPrequest ): String = {
      s"""Error '$specificMessage', retry later !!!!!!!!
        ${request.uri}
          ${t.getLocalizedMessage}
          ${printMemory}"""
  }
}
