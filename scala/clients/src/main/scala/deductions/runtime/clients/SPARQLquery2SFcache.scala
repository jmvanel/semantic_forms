package deductions.runtime.clients

import java.net.URLEncoder

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.apache.jena.query.QueryExecutionFactory
import org.w3.banana.RDFOps
import org.w3.banana.SparqlOps
import org.w3.banana.jena.Jena

/** SPARQL query to Semantic_Forms cache */
trait SPARQLquery2SFcache {
  val serverPrefixWithParam = "/load-uri?uri="
  // "/display?displayuri="
  var num = 0

  implicit val ops: RDFOps[Jena]
  implicit val sparqlOps: SparqlOps[Jena]

  /** import into  Semantic_Forms From SPARQL Query to any SPARQL endpoint
   *  @param sfInstancePrefix Semantic_Forms Instance URL Prefix
   *  ( #serverPrefixWithParam will be appended)  */
  def importFromQuery(query: String, endpoint: String, sfInstancePrefix: String): String = {
    val uris = sendQuery(query, endpoint)
    val results = for (uri <- uris) yield {
      sendGetToSemantic_Forms(uri, sfInstancePrefix)
    }
    results.mkString("")
  }

  import scala.collection.JavaConversions._
  val varName = "sub" // "URI"
  def sendQuery(queryString: String, endpoint: String): Iterator[String] = {
    import sparqlOps._
    // Banana
    val solutionsTry = for (
      query <- parseSelect(queryString);
      queryExecution = QueryExecutionFactory.sparqlService(endpoint, query);
      resultSet = queryExecution.execSelect()
    ) yield {
      resultSet
    }
    val solutions = solutionsTry.get

    val rr = for (
      solution <- solutions;
      x = solution.get(varName);
      res = x.asResource() ;
      uri = res.getURI
    ) yield uri
    rr
  }

  /** send HTTP Get To Semantic_Forms */
  def sendGetToSemantic_Forms(
    uri: String, sfInstancePrefix: String): String = {
    val httpclient = HttpClients.createDefault()

    val httpGet = new HttpGet(sfInstancePrefix + serverPrefixWithParam + URLEncoder.encode(uri, "UTF-8"))
    val response1 = httpclient.execute(httpGet);
    // The underlying HTTP connection is still held by the response object
    // to allow the response content to be streamed directly from the network socket.
    // In order to ensure correct deallocation of system resources
    // the user MUST call CloseableHttpResponse#close() from a finally clause.
    // Please note that if response content is not fully consumed the underlying
    // connection cannot be safely re-used and will be shut down and discarded
    // by the connection manager.
    try {
      num = num + 1
      println(num + " " + uri + " --> " + response1.getStatusLine() +
          "\t" + httpGet)
      val entity1 = response1.getEntity()

      // do something useful with the response body
      // get HTTP status
      val statusCode = response1.getStatusLine().getStatusCode()
      val result =
        if (statusCode != 200)
          "KO: " + response1.getStatusLine().toString() + "\n"
        else
          "OK: " + response1.getStatusLine().toString() + "\n"
      // and ensure it is fully consumed
      EntityUtils.consume(entity1)
      result
    } finally {
      response1.close();
    }
  }
}