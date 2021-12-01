package jmvanel
import org.apache.jena.tdb.TDBFactory
import org.apache.jena.query._
import scala.jdk.CollectionConverters._
import java.io.FileWriter
import java.io.File

object GMLexport extends App {
  // Open TDB db
  val dataset = TDBFactory.createDataset(args(0))
  
  // SPARQL query
  val queryString = """
  PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>  
  CONSTRUCT {
    ?S geo:lat ?LAT .
    ?S geo:long ?LON .
    # ?S ?P ?O .
    }
  WHERE {
    GRAPH ?gr {
    ?S geo:lat ?LAT .
    ?S geo:long ?LON .
    # ?S ?P ?O .
    }
  }
  """
  val fileName = "out.gml"

  val geoPref = "http://www.w3.org/2003/01/geo/wgs84_pos#"
    val qexec = QueryExecutionFactory.create(queryString, dataset)
    val resultModel = qexec.execConstruct()
    val subjects = resultModel.listSubjects()
    val ss = ( subjects . toList() . asScala )
    val latProp = resultModel.getProperty(geoPref + "lat")
    val longProp = resultModel.getProperty(geoPref + "long")

    val features = for ( subject <- ss ) yield {
      val lat =  subject.getProperty(latProp) .getString()
      val long = subject.getProperty(longProp).getString()
      println(s"subject $subject")
      makeOutput(subject.getURI, lat, long)
    }
  val fw = new FileWriter(fileName)
  // println(s"fw $fw")
  fw.write("""<ogr:FeatureCollection
     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
     xmlns:ogr="http://ogr.maptools.org/"
     xmlns:gml="http://www.opengis.net/gml">   
    """)
  fw.write(features.mkString("\n"))
  fw.write("</ogr:FeatureCollection>\n")
  fw.close()
  println(s"file '$fileName' generated in ${new File(".").getAbsolutePath()}")

  def makeOutput(uri: String, lat: String, long: String) = {
    <gml:Point gml:id={uri} srsName="http://www.opengis.net/def/crs/EPSG/0/4326">
      <gml:pos srsDimension="2">{long} {lat}</gml:pos>
    </gml:Point>
  }
 // write points
 // write other data
}
