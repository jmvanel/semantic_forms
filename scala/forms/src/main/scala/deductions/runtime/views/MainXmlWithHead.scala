package deductions.runtime.views

import scala.io.Source
import scala.xml.Elem
import scala.xml.Node
import scala.xml.XML

//import controllers.routes
// import deductions.runtime.views.MainXml
import scala.xml.NodeSeq
import org.w3.banana.RDF
import java.io.File

/** HTML page skeleton */
trait MainXmlWithHead[Rdf <: RDF, DATASET] extends MainXml[Rdf, DATASET] {

  private def add(e: Elem, c: Node): Elem = e.copy(child = e.child ++ c)
  private def addFile(name: String): NodeSeq = {
    scala.xml.Unparsed(
      if( new File(name).exists() )
          Source.fromFile(name).getLines .mkString("\n")
      else
        // this way, head.html is in forms/ module, removing a dependency in forms_play/ module
      Source.fromURL(getClass.getResource("/deductions/runtime/html/" + name)).getLines().mkString("\n")
    )
  }

  private val basicHead = addFile("head.html")

  /** HTML head content */
  override def head(title: String = "")(implicit lang: String = "en"): NodeSeq = {
    val titleTag =
      <title>
        {
          val default = messageI18N("Welcome")
          if (title != "")
            s"$title - $default"
          else
            default
        }
      </title>
    basicHead ++
    titleTag ++
    <meta name="Description" content={title}/>
  }

  /** Technical stuff (JS) that needs to be after page <body> */
  override def tail(): NodeSeq = addFile("tail.html")
}
