#@namespace scala pingpong.protocol.pingpong

include "entities.thrift"
include "notifications.thrift"
include "repo_backend.thrift"

struct RegionComment {
  1: optional repo_backend.PingLocation ping_location;
}

# E.g. a comment on the pull request itself, not tied to a particular line.
struct ThreadComment {}

struct Reply {
  # This should be local to the containing `PingCollection`. Applications can decide
  # what to do if it is not contained in there (probably error out).
  # Applications should follow the thread of `Reply` pings through `parent` ids until reaching a
  # `RegionComment` or `ThreadComment`.
  1: optional entities.PingId parent;
}

union PingSource {
  1: RegionComment region_comment;
  2: ThreadComment thread_comment;
  3: Reply reply;
}

struct Ping {
  1: optional PingSource source;
  2: optional notifications.TargetSpecification notifies;
  # NB: It should be enforced at the application level some approval permissions model,
  # e.g. "approvals must be done in a ping that replies (directly?) to the ping being approved".
  3: optional notifications.ApprovalSpecification approves;
  4: optional entities.UserId author;
  # This is just the text a human wrote -- individual backends may add additional text, e.g. adding
  # a github @user in the comment text for all the users that need to be notified. Backends should
  # ensure they return exactly the comment_text of the Ping they received, or update the
  # comment_text of the ping at that id.
  5: optional string comment_text;
}

# NB: This is for reading/writing by clients, and *all* `PingId` instances are local to this map!
typedef map<entities.PingId, Ping> PingCollection

# TODO: With the use of `PingCollection`, the below is unnecessary. Scrutiny should be determined by
# the frontend!!!
# # "scrutiny" as a term and a concept is my interpretation of what is described in
# # https://blog.janestreet.com/putting-the-i-back-in-ide-towards-a-github-explorer/.
# enum Scrutiny {
#   # No approval necessary.
#   COMMENT = 0,
#   # Explicit approval is necessary.
#   REVIEW_REQUEST = 1,
# }

# # A ping, but directed at you.
# struct Pong {
#   # This is for the purposes of the Reply struct above.
#   1: optional entities.PingId ping_id;
#   2: optional Ping ping;
#   3: optional Scrutiny scrutiny;
# }
