package pingpong.parsing

import com.twitter.scrooge.ThriftStruct
import com.twitter.util.Try

trait Thriftable[T <: ThriftStruct] {
  def asThrift: T
}

trait ThriftParseable[T <: ThriftStruct, S <: Thriftable[T]] {
  def apply(struct: T): Try[S]
}

trait StringParseable[T <: ThriftStruct, S <: Thriftable[T]] extends ThriftParseable[T, S] {
  def apply(str: String): Try[S]
}

// NB: we could use implicits/typeclasses here, but `Thriftable` doesn't cover a lot of
// functionality, so it's probably better to explicitly invoke the type for now.
// object ThriftExt {
//   implicit class ThriftWrapper[T <: ThriftStruct](thriftObj: T) {
//     def parse[S <: Thriftable[T]](
//       implicit parser: ThriftParseable[T, S]
//     ): Try[S] = parser(thriftObj)
//   }
//   implicit class ThriftStringWrapper(str: String) {
//     def parseWrappedThrift[T <: ThriftStruct, S <: Thriftable[T]](
//       implicit parser: StringParseable[T, S]
//     ): Try[S] = parser(str)
//   }
// }
