package deductions.runtime.views

import deductions.runtime.utils.Configuration

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.{Elem, NodeSeq}

trait Results {
  val config: Configuration
  import config._

  /** Add header and wrapper tag to Search Results
   *  @param query string or, in the case of backlinks, URI */
  def wrapSearchResults(fut: Future[NodeSeq], query: String,
      mess: NodeSeq = <div>Searched for</div>): Future[Elem] =
    fut.map { v =>
      <section class="label-search-results">
        <p class="label-search-header">{ mess } "{ query }" :</p>
        <div>
          { css.localCSS }
          { v }
        </div>
      </section>
    }
}