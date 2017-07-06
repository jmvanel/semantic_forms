package deductions.runtime.jena

import org.apache.jena.rdf.model.ModelFactory
import org.w3.banana.{RDF, RDFOps}
import org.w3.banana.jena.{Jena, JenaOps}

import scala.util.Try

trait MicrodataLoaderModule[Rdf <: RDF] {
  val microdataLoader: MicrodataLoader[Rdf]
}

trait MicrodataLoaderModuleJena extends MicrodataLoaderModule[Jena] {
  override lazy val microdataLoader = new MicrodataLoaderJena{
    val ops: RDFOps[Rdf] = new JenaOps
  }
}

trait MicrodataLoader[Rdf <: RDF] {
  def load(url: java.net.URL): Try[Rdf#Graph]  
}

trait MicrodataLoaderJena extends MicrodataLoader[Jena] {
  type Rdf = ImplementationSettings.Rdf
  val ops: RDFOps[Rdf]
  import ops._

  {
    try {
      Class.forName("net.rootdev.javardfa.jena.RDFaReader")
      // Which will hook the two readers in to jena, then you will be able to read RDFa URL
      println("""Class.forName("net.rootdev.javardfa.jena.RDFaReader")""")
    } catch {
      case t: Throwable =>
        // for Java-RDFa release 0.4.2 :
        println(s"RDFaReader could not be instanciated, exception: $t")
    }
  }

  def load(url: java.net.URL): Try[Rdf#Graph] = {
    val graph = emptyGraph
    val model = ModelFactory.createModelForGraph(graph)
    Try {
      //model.read(url, "XHTML"); // xml parsing
      println(s"""MicrodataLoaderJena: before model.read(url""")
      model.read(url.toExternalForm(), "HTML") // html parsing
      println(s"""MicrodataLoaderJena: AFTER model.read(url, model.size ${model.size()}""")
      graph
    }
  }
}
