package deductions.runtime

import org.scalatest._
import org.scalatest.matchers._
import org.scalatest.selenium._
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.By
import scala.collection.JavaConverters._

//import org.openqa.selenium._
/** see http://doc.scalatest.org/1.7/org/scalatest/matchers/ShouldMatchers.html */

/** NOTE:
 *  It's not easy to set HTTP headers, so this is for browser set to french language
 *
 *  See ../web_tests/crud.selenium.xhtml ,
 *   doc. http://www.scalatest.org/user_guide/using_selenium */
trait CRUD extends TestBoilerPlate {

  def createPersonTest = {
    go to (host + "")

    val toggleButton = xpath(" //button [ @id='toggleCreate']")
    click on toggleButton
    val buttonCreate = xpath(" //input [ @id='sf-button-create']")
    println("buttonCreate " + buttonCreate)
    click on buttonCreate

    // fill form

    val inputFirstName = xpath("""//form // div [ label [
        . // text() = "prénom" or
        . // text() = "firstName" or
        . // text() = "Given name"
      ]] // input [ @type != "button" ]
    """)
    click on inputFirstName
    textField(inputFirstName).value = "Victime 2"
    println( "condition " + textField(inputFirstName) )

    val inputKnows = "// input [ @data-rdf-property = 'http://xmlns.com/foaf/0.1/knows' ]"
    click on inputKnows
    textField(inputKnows).value = "Jean-Marc"
    selectOptionWithText("Jean-Marc Vanel")
    // After completion:
    textField(inputKnows).value should be ("http://jmvanel.free.fr/jmv.rdf#me")

    val SaveButton = xpath( "//input[@value='SAUVER']" )
    Thread.sleep(20)

    // check page content: rdf:type, foaf:firstName, foaf:knows
    // page should contain "Violette" (twice, in h3 and in span), a link named "Jean-Marc Vanel",
    // Spécification de formulaire: Personne FOAF - court,
    // type: Personne
    val nameCount = findAll(xpath("""//*[text() = "Violette"]"""))
    nameCount should have size (2)
    linkText("Jean-Marc Vanel")
    linkText("Personne FOAF - court")
    linkText("Personne")
  }

  /** WIP, taken from
   *  http://www.seleniumeasy.com/selenium-tutorials/working-with-ajax-or-jquery-auto-complete-text-box-using-webdriver */
  def selectOptionWithText(textToSelect: String) {
    /*WebElement*/ val autoOptions = webDriver.findElement(By.id("ui-id-1"));
    webDriverWait.until(ExpectedConditions.visibilityOf(autoOptions));

    /*List<WebElement>*/ val optionsToSelect = autoOptions.findElements(By.tagName("li"))
    var break = false
    for ( /*WebElement*/ option <- optionsToSelect.asScala) {
      if (option.getText().equals(textToSelect) && !break) {
        System.out.println("Trying to select: " + textToSelect);
        option.click();
        break = true
      }
    }
  }
}
