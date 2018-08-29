package deductions.runtime.abstract_syntax.apps

import java.io.File

import deductions.runtime.abstract_syntax.FormSpecificationsFromVocab
import deductions.runtime.jena.{ImplementationSettings, RDFStoreLocalJena1Provider}
import deductions.runtime.utils.DefaultConfiguration

import scala.util.Try
import scalaz._
import Scalaz._
import deductions.runtime.utils.FormModuleBanana

/**
  * Created by LaFaucheuse on 07/07/2017.
  */
object FormSpecificationsFromVocabApp extends RDFStoreLocalJena1Provider
  with App
  with FormSpecificationsFromVocab
    [ImplementationSettings.Rdf, ImplementationSettings.DATASET]
  with FormModuleBanana[ImplementationSettings.Rdf] {

  val config = new DefaultConfiguration {
    override val useTextQuery = false
  }

  if (args.size === 0) {
    println("Usage: input file for vocabulary; output in new file with '.formspec.ttl' suffix")
    System.exit(-1)
  }
  val file = new File(args(0))
  val res: Try[Rdf#Graph] = if( file.exists() ) {
    readFile(file)
  } else
    readTurtleTerm(args(0))

  makeFormSpecificationsFromVocabFile( res, file )
}
