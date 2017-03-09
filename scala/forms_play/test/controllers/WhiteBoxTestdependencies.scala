package controllers

import org.w3.banana.RDFOpsModule
import org.w3.banana.SparqlGraphModule
import org.w3.banana.TurtleWriterModule
import deductions.runtime.jena.ImplementationSettings

trait WhiteBoxTestdependencies extends ImplementationSettings.RDFModule
    with ImplementationSettings.RDFCache
    with RDFOpsModule
    with SparqlGraphModule
    with TurtleWriterModule {
  
}