package deductions.runtime.abstract_syntax

import deductions.runtime.core.FormModule
import deductions.runtime.semlogs.TimeSeries
import org.w3.banana.RDF

import scala.collection.mutable
import scala.language.{existentials, postfixOps}


trait UserTraceability[Rdf <: RDF, DATASET]
  extends FormModule[Rdf#Node, Rdf#URI]
    with TimeSeries[Rdf, DATASET] {

  def addUserInfoOnTriples(
                            formSyntax: FormSyntax)(
                            implicit graph: Rdf#Graph,
                            lang: String = "en") : FormSyntax = {
    println("XXXXXXXXXXXXXXXX addUserInfoOnTriples")
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
          resultsUser put (property, row(3).toString)
        }
      }
      else{
        resultsTimestamp put (property,timeElement)
        resultsUser put (property,row(3).toString)
      }
    }
    for (elem <- resultsTimestamp){
    	println("\t" + elem)
    }

//    println(s"YYYYYYYY Before add User Info\n${formSyntax.fields.mkString("\n")}\n")
    val entries = for (field: Entry <- formSyntax.fields) yield {
      if (resultsUser.contains(field.property.toString)){
//    	  println(s"ZZZZ add User Info ${field.label} ${field.value}")
        field.makeEntry(
            fromMetadata = resultsUser(field.property.toString),
            fromTimeMetadata = resultsTimestamp(field.property.toString) )
      } else field
    }
    formSyntax . fields = entries
    formSyntax
  }
}