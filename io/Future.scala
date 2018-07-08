package pingpong.io

import com.twitter.util.{Try, Future}

object FutureTryExt {
  implicit class FutureSeq[T](futures: Seq[Future[T]]) {
    def collectFutures = Future.collect(futures)
  }

  implicit class TryWrapper[T](theTry: Try[T]) {
    def constFuture = Future.const(theTry)
    def join[S](otherTry: Try[S]): Try[(T, S)] = theTry.flatMap(res => otherTry.map((res, _)))
  }

  implicit class TrySeq[T](tries: Seq[Try[T]]) {
    def collectTries = Try.collect(tries)
  }
}
