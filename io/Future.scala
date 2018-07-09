package pingpong.io

import com.twitter.util.{Try, Return, Future}

object FutureTryExt {
  type ExceptionFactory[T] = String => Try[T]

  implicit class OptionalFieldDeref[T](opt: Option[T]) {
    def derefOptionalField(fieldName: String)(implicit factory: ExceptionFactory[T]): Try[T] =
      opt.map(Return(_)).getOrElse(factory(fieldName))
  }

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
