package pingpong.parsing

import com.twitter.bijection._
import com.twitter.bijection.Conversion.asMethod
import com.twitter.bijection.twitter_util.UtilBijections._
import com.twitter.scrooge.ThriftStruct
import com.twitter.util.{Try, Throw}

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

  def thriftParseErrorWrapper(description: String, arg: Thrift, theTry: Try[S]): Try[S]

  // TODO: test whether round-trip parsing is equal
  def asThriftParse[X <: S](description: String, arg: Thrift, theTry: Try[S]): ThriftParse = {
    val wrappedTry = thriftParseErrorWrapper(description, arg, theTry)
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

  def stringParseErrorWrapper(description: String, arg: String, theTry: Try[S]): Try[S]

  // TODO: test whether round-trip parsing is equal
  def asStringParse(description: String, arg: String, theTry: Try[S]): StringParse = {
    val wrappedTry = stringParseErrorWrapper(description, arg, theTry)
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
