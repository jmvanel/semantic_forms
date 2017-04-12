package deductions.runtime.abstract_syntax

import deductions.runtime.semlogs.TimeSeries

import scala.language.existentials
import scala.language.postfixOps
import org.w3.banana.RDF

import scala.collection.mutable


trait UserTraceability[Rdf <: RDF, DATASET]
  extends FormModule[Rdf#Node, Rdf#URI]
    with TimeSeries[Rdf, DATASET] {

  def addUserInfoOnTriples(
                            formSyntax: FormSyntax)(
                            implicit graph: Rdf#Graph,
                            lang: String = "en") : FormSyntax = {
    println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX")
    println()
    val metadata = getMetadataAboutSubject(formSyntax.subject)
    val resultsUser = mutable.Map[String,String]()
    val resultsTimestamp = mutable.Map[String,Long]()

    for (row: Seq[Rdf#Node] <- metadata){
      println(row)
      val timeElementStr = row(2).toString
      val timeElement  = timeElementStr.splitAt(timeElementStr.indexOf("^"))._1.replaceAll("\"","").toLong
      val property = row(0).toString

      if(resultsTimestamp.contains(property)){
        print(resultsTimestamp(property))
        print(" < ")
        print(timeElement)
        print(" = ")
        println(resultsTimestamp(property) < timeElement)
        if(resultsTimestamp(property) < timeElement){
          resultsTimestamp += (property -> timeElement)
          resultsUser put (property,row(3).toString)
        }
      }
      else{
        resultsTimestamp put (property,timeElement)
        resultsUser put (property,row(3).toString)
      }
    }

    println()
    for (elem <- resultsTimestamp){
    println(elem)
    }
    println()

    for (field: Entry <- formSyntax.fields){
      if (resultsUser.contains(field.property.toString)){
        field.metadata = resultsUser(field.property.toString)
        field.timeMetadata = resultsTimestamp(field.property.toString)
      }
    }

    println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX")

    formSyntax
  }
}