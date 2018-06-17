include "entities.thrift"
include "notifications.thrift"
include "repo_backend.thrift"
include "review_backend.thrift"

struct Root {
  1: optional repo_backend.PingLocation ping_location;
}

struct Reply {
  1: optional entities.PingId parent;
  # This can be obtained from ping_source, and may be unset. It is an application-level error to set
  # when posting a Ping.
  2: optional repo_backend.PingLocation ping_location;
}

union PingSource {
  1: optional Root root;
  2: optional Reply reply;
}

struct Ping {
  1: optional entities.CollaborationId collaboration_id;
  # This may be unset.
  2: optional PingSource ping_source;
  4: optional notifications.TargetSpecification notifies;
  # NB: It should be enforced at the application level some approval permissions model,
  # e.g. "approvals must be done in a ping that replies (directly?) to the ping being approved".
  5: optional notifications.ApprovalSpecification approves;
  6: optional entities.UserId author;
  # This is just the text a human wrote -- individual backends may add additional text, e.g. adding
  # a github @user in the comment text for all the users that need to be notified. Backends should
  # ensure they return exactly the comment_text of the Ping they received, or update the
  # comment_text of the ping at that id.
  7: optional string comment_text;
}

# "scrutiny" as a term and a concept is my interpretation of what is described in
# https://blog.janestreet.com/putting-the-i-back-in-ide-towards-a-github-explorer/.
enum Scrutiny {
  # No approval necessary.
  COMMENT = 0,
  # Explicit approval is necessary.
  REVIEW_REQUEST = 1,
}

# A ping, but directed at you.
struct Pong {
  1: optional entities.PingId ping_id;
  # This field may be unset.
  2: optional Ping ping;
  3: optional Scrutiny scrutiny;
}
