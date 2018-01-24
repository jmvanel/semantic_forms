import org.scalawebtest.core.IntegrationFlatSpec
import org.scalawebtest.core.gauge.HtmlGauge

class HomepageSpec extends IntegrationFlatSpec with HtmlGauge {
  override val host = "http://localhost:9000"
  path = ""

  "Our homepage" should "contain a succinct claim" in {
    fits(
      <h1> Welcome to Semantic forms </h1>)
  }
}
