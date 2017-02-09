package deductions.runtime.views

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.Elem
import scala.xml.NodeSeq

import deductions.runtime.services.Configuration

trait Results {
  val config: Configuration
  import config._

  def wrapSearchResults(fut: Future[NodeSeq], q: String, mess: String = "Searched for"): Future[Elem] =
    fut.map { v =>
      <section class="label-search-results">
        <p class="label-search-header">{ mess } "{ q }" :</p>
        <div>
          { css.localCSS }
          { v }
        </div>
      </section>
    }
}