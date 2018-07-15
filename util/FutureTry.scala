package pingpong.util

import com.twitter.util.{Try, Return, Future}

object FutureTryExt {
  type ExceptionFactory[T] = String => Try[T]

  implicit class OptionalFieldDeref[T](opt: Option[T]) {
    def derefOptionalField(fieldName: String)(implicit factory: ExceptionFactory[T]): Try[T] =
      opt.map(Return(_)).getOrElse(factory(fieldName))
  }

  // NB: Use this where possible over `constFuture` to make it clearer what's synchronous and what's
  // not. `constFuture` is so that a `Try` can be `flatMap`'d with a subsequent `Future`.
  implicit class FutureWrapper[T](future: Future[T]) {
    def flatTry[S](f: T => Try[S]): Future[S] = future.flatMap(x => Future.const(f(x)))
  }

  implicit class FutureSeq[T](futures: Seq[Future[T]]) {
    def collectFutures = Future.collect(futures)
  }

  implicit class TryWrapper[T](theTry: Try[T]) {
    def constFuture = Future.const(theTry)
    def join[S](otherTry: Try[S]): Try[(T, S)] = theTry.flatMap(res => otherTry.map((res, _)))
  }

  // TODO: coalesce `TryOption` and `TrySeq` with some generic implicit like `CanBuildFrom`.
  implicit class TryOption[T](tryOpt: Option[Try[T]]) {
    def flipTryOpt = tryOpt match {
      case Some(theTry) => theTry.map(Some(_))
      case None => Return(None)
    }
  }

  implicit class TrySeq[T](tries: Seq[Try[T]]) {
    def collectTries = Try.collect(tries)
  }
}
