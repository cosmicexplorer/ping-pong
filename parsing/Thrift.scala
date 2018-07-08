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
