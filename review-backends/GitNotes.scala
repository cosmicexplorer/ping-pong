package pingpong.review_backends

import pingpong.parsing.Regex._
import pingpong.protocol.entities._
import pingpong.protocol.pingpong._
import pingpong.protocol.review_backend._

import com.twitter.util.{Try, Return, Throw}

class GitNotesError(message: String, base: Throwable) extends RuntimeException(message, base) {
  def this(message: String) = this(message, null)
}

case class GitIdentificationError(message: String) extends GitNotesError(message)

// TODO: what other notions of identity should we consider? This is probably fine for a first draft.
case class GitUser(email: String) {
  def asThrift = UserId(Some(email))
}

object GitUser {
  // From https://stackoverflow.com/a/13087910/2518889 -- should use a more legitimate solution, but
  // email as the sole identifier should be revised anyway.
  val emailRegex = rx"\A([0-9a-zA-Z](?>[-.\w]*[0-9a-zA-Z])*@(?>[0-9a-zA-Z][-\w]*[0-9a-zA-Z]\.)+[a-zA-Z]{2,9})\Z"

  def apply(gitId: UserId): Try[GitUser] = Try {
    gitId.uid
      .map(GitUser(_).get)
      .head
  }

  def apply(email: String): Try[GitUser] = {
    emailRegex.findFirstIn(email) match {
      case Some(validEmail) => Return(new GitUser(validEmail))
      case None => Throw(GitIdentificationError(
        s"invalid email ${email}: email address must be provided and match ${emailRegex}"))
    }
  }
}

// TODO: where do we store
