import org.scalawebtest.core.IntegrationFlatSpec

                    class HomepageSpec extends IntegrationFlatSpec {
                        override val host = "http://www.scalawebtest.org"
                        path = "/index.html"

                        "Our homepage" should "contain a succinct claim" in {
                            webDriver.findElementByTagName("h2").getText shouldEqual "Reduce the effort needed to write integration tests"
                        }
                    }
