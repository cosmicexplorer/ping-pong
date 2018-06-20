// package pingpong.protocol.frontend

// // This doesn't work yet. I don't think I want to use this library. I would LOVE

// import org.json4s._
// import org.json4s.jackson.JsonMethods._
// import sangria.execution._
// import sangria.macros._
// import sangria.macros.derive._
// import sangria.marshalling.json4s.jackson._
// import sangria.schema._

// import scala.concurrent.ExecutionContext

// // https://sangria-graphql.org/getting-started

// case class SomeText(text: String)

// val InputText = Argument("inputText", StringType)

// object SomeText {
//   implicit lazy val SomeTextType = deriveObjectType[Unit, SomeText](
//     ObjectTypeDescription("idk"),
//     DocumentField("text", "huh"))
// }

// trait Appendable {
//   def append(other: String): SomeText
// }

// object Appendable {
//   // FIXME: needs its own macro!
//   implicit lazy val AppendableType = InterfaceType(
//     "Appendable",
//     "wowza",
//     () => fields[Appendable, SomeText.inputText](
//       Field(
//         "append",
//         SomeText.SomeTextType,
//         arguments=(SomeText.inputText :: Nil),
//         resolve=(c => c.ctx.append(c.arg(SomeText.inputText))))))
// }

// case class TextQuery(init: String) extends Appendable {
//   override def append(other: String): SomeText = SomeText(init + other)
// }

// object TextQuery {
//   implicit lazy val TextQueryType = deriveObjectType[TextQuery, SomeText.inputText](
//     Interfaces(Appendable.AppendableType),
//     IncludeMethods("append"))
// }

// object GraphQLSchema {
//   val schema = Schema(TextQuery.TextQueryType)

//   val query = graphql"""
//     query TextQuery {
//       append(inputText: "sick") {
//         text
//       }
//     }
//   """

//   def queryResult(implicit ec: ExecutionContext) =
//     Executor.execute(schema, query, new TextQuery("what"))
// }
