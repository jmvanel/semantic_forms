package deductions.runtime.utils

import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global

object MonadicHelpers {

  // cf http://stackoverflow.com/questions/17907772/scala-chaining-futures-try-blocks
  def tryToFuture[T](t: Try[T]): Future[T] =
    t match {
      case Success(s) => Future.successful(s)
      case Failure(ex) => Future.failed(ex)
    }

  def tryToFutureFlat[T](t: Try[Future[T]]) = {
    tryToFuture(t).flatMap { identity }
  }
}