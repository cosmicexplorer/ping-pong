package pingpong.parsing

import com.twitter.scrooge.ThriftStruct
import com.twitter.util.{Try, Throw}

import scala.reflect.ClassTag

trait Thriftable[T <: ThriftStruct] {
  def asThrift: T
}

case class ThriftParseResult[T <: ThriftStruct, S <: Thriftable[T]](theTry: Try[S]) {
  def asTry = theTry
}

trait ThriftParser[Thrift <: ThriftStruct, S <: Thriftable[Thrift]] {
  type ThriftParse = ThriftParseResult[Thrift, S]

  def apply(thriftStruct: Thrift): ThriftParse

  type DescriptiveThriftParseErrorFactory[E <: Throwable, F <: Throwable] = (String, Thrift, E) => F

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

case class StringParseResult[S](theTry: Try[S]) {
  def asTry = theTry
}

trait StringParser[S] {
  type StringParse = StringParseResult[S]

  def apply(str: String): StringParse

  type DescriptiveStringParseErrorFactory[E <: Throwable, F <: Throwable] = (String, String, E) => F

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
