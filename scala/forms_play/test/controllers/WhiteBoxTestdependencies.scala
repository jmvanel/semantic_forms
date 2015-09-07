package controllers

import org.w3.banana.RDFOpsModule
import org.w3.banana.SparqlGraphModule
import org.w3.banana.TurtleWriterModule
import org.w3.banana.jena.JenaModule

import deductions.runtime.jena.RDFStoreLocalJena1Provider

trait WhiteBoxTestdependencies extends JenaModule
    with RDFStoreLocalJena1Provider
    with RDFOpsModule
    with SparqlGraphModule
    with TurtleWriterModule {
  
}