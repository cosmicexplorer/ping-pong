package pingpong.parsing

import com.twitter.bijection._
import com.twitter.bijection.Conversion.asMethod
import com.twitter.bijection.twitter_util.UtilBijections._
import com.twitter.scrooge.ThriftStruct
import com.twitter.util.{Try, Throw}

import scala.reflect.ClassTag

trait Thriftable[T <: ThriftStruct] {
  def asThrift: T
}

trait HasCanonicalString {
  def asCanonicalString: String
}

case class ThriftParseResult[T <: ThriftStruct, S <: Thriftable[T]](theTry: Try[S]) {
  def asTry = theTry
}

trait ThriftParser[Thrift <: ThriftStruct, S <: Thriftable[Thrift]] {
  type ThriftParse = ThriftParseResult[Thrift, S]

  def apply(thriftStruct: Thrift): ThriftParse

  type DescriptiveThriftParseErrorFactory[E <: Throwable, F <: Throwable] = (String, Thrift, E) => F

  // TODO: test whether round-trip parsing is equal
  def asThriftParse[E <: Throwable, F <: Throwable](
    description: String, arg: Thrift, theTry: Try[S]
  )(
    implicit classTagE: ClassTag[E], factory: DescriptiveThriftParseErrorFactory[E, F]
  ): ThriftParse = {
    val wrappedTry = theTry.rescue { case e: E if classTagE.runtimeClass.isInstance(e) =>
      Throw(factory(description, arg, e))
    }
    ThriftParseResult(wrappedTry)
  }
}

// FIXME: use bijections later -- also use for thrift struct enc/decoding in `pingpong.io`!
// class ThriftParseInjection[Thrift <: ThriftStruct, S <: Thriftable[Thrift]](
//   parser: ThriftParser[Thrift, S]
// ) extends Injection[S, Thrift] {
//   override def apply(s: S) = s.asThrift
//   override def invert(t: Thrift) = parser.apply(t).asTry.as[scala.util.Try[S]]
// }

case class StringParseResult[S](theTry: Try[S]) {
  def asTry = theTry
}

trait StringParser[S <: HasCanonicalString] {
  type StringParse = StringParseResult[S]

  def apply(str: String): StringParse

  type DescriptiveStringParseErrorFactory[E <: Throwable, F <: Throwable] = (String, String, E) => F

  // TODO: test whether round-trip parsing is equal
  def asStringParse[E <: Throwable, F <: Throwable](
    description: String, arg: String, theTry: Try[S]
  )(
    implicit classTagE: ClassTag[E], factory: DescriptiveStringParseErrorFactory[E, F]
  ): StringParse = {
    val wrappedTry = theTry.rescue { case e: E if classTagE.runtimeClass.isInstance(e) =>
      Throw(factory(description, arg, e))
    }
    StringParseResult(wrappedTry)
  }
}

// NB: we could use implicits/typeclasses here, but `Thriftable` doesn't cover a lot of
// functionality, so it's probably better to explicitly invoke the type for now.
// object ThriftExt {
//   implicit class ThriftWrapper[T <: ThriftStruct](thriftObj: T) {
//     def parse[S <: Thriftable[T]](
//       implicit parser: ThriftParser[T, S]
//     ): Try[S] = parser(thriftObj)
//   }
//   implicit class ThriftStringWrapper(str: String) {
//     def parseWrappedThrift[T <: ThriftStruct, S <: Thriftable[T]](
//       implicit parser: StringParser[T, S]
//     ): Try[S] = parser(str)
//   }
// }
