package pingpong.protocol.frontend

import sangria.macros.derive._

case class SomeText(text: String)

object SomeText {
  implicit val SomeTextType = deriveObjectType[Unit, SomeText](
    ObjectTypeDescription("idk"),
    DocumentField("text", "huh"))
}
