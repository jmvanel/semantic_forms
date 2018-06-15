package deductions.runtime.jena

import org.apache.jena.rdf.model.ModelFactory
import org.w3.banana.{RDF, RDFOps}
import org.w3.banana.jena.{Jena, JenaOps}

import scala.util.Try
import deductions.runtime.sparql_cache.MicrodataLoader
import deductions.runtime.sparql_cache.MicrodataLoaderModule
import net.rootdev.javardfa.jena.JenaStatementSink
import net.rootdev.javardfa.ParserFactory
import net.rootdev.javardfa.uri.URIResolver
import net.rootdev.javardfa.ParserFactory.Format
import scala.util.Success
import net.rootdev.javardfa.Setting

trait MicrodataLoaderModuleJena extends MicrodataLoaderModule[Jena] {
  override lazy val microdataLoader = new MicrodataLoaderJena{
    val ops: RDFOps[Rdf] = new JenaOps
  }
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
    Success(emptyGraph)

    // DEACTIVATED before rdfa-java 4.3

//    val graph = emptyGraph
//    val model = ModelFactory.createModelForGraph(graph)
//    val sink = new JenaStatementSink(model)
//
//    // TODO RDFa settings
//    val format = Format.XHTML;
////    val reader = ParserFactory.createReaderForFormat(sink, format, new URIResolver());
//    val reader = ParserFactory.createReaderForFormat(sink, Format.HTML, Setting.OnePointOne);
//
//    /* TODO set HTTP connection on connection object, somtheting like
//     * URL url = new URL("http://www.something.com/blah.xml");
//       ParserAdapter xml = new ParserAdapter();
//       xml.parse(new InputSource(url.openStream()));
//       or see
//       https://stackoverflow.com/questions/16840365/set-timeout-on-url-openstream-android
//    */
//    // to avoid HTTP 403 from server; alternatively maybe set agent on the URL connection
//    System.setProperty("http.agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36     (KHTML, like Gecko) Chrome/28.0.1500.29 Safari/537.36");
//    val defaultReadTimeout = "40000"
//    val defaultConnectTimeout = "40000"
//    System.setProperty("sun.net.client.defaultReadTimeout", defaultReadTimeout)
//    System.setProperty("sun.net.client.defaultConnectTimeout", defaultConnectTimeout)
//
//    reader.parse(url.toExternalForm())
//    Success(model.getGraph.asInstanceOf[Rdf#Graph])
  }

  def loadOLD(url: java.net.URL): Try[Rdf#Graph] = {
    val graph = emptyGraph
    val model = ModelFactory.createModelForGraph(graph)
    Try {
      //model.read(url, "XHTML"); // xml parsing
      println(s"""MicrodataLoaderJena: before model.read(url""")
      model.read(url.toExternalForm(), "XHTML") // html parsing
      println(s"""MicrodataLoaderJena: AFTER model.read(url, model.size ${model.size()}""")
      graph
    }
  }
}
