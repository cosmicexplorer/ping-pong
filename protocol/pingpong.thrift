#@namespace scala pingpong.protocol

include "entities.thrift"
include "notifications.thrift"
include "repo_backend.thrift"

# NB: we don't use "required" ever!

struct Ping {
  1: optional entities.CollaborationId collaboration_id;
  2: optional repo_backend.PingLocation ping_location;
  3: optional notifications.TargetSpecification notifies;
  # NB: It should be enforced at the application level some approval permissions model,
  # e.g. "approvals must be done in a ping that replies (directly?) to the ping being approved".
  4: optional notifications.ApprovalSpecification approves;
  5: optional entities.UserId author;
  # This is just the text a human wrote -- individual backends may add additional text, e.g. adding
  # a github @user in the comment text for all the users that need to be notified. Backends should
  # ensure they return exactly the comment_text of the Ping they received, or update the
  # comment_text of the ping at that id.
  6: optional string comment_text;
}

struct RootPingRequest {
  # If this is not provided, this is a top-level comment not associated with any file (this would
  # become e.g. a github comment in the main thread).
  1: optional repo_backend.Hunk pertinent_range;
  2: optional Ping ping;
}

struct ReplyPingRequest {
  1: optional entities.PingId parent;
  3: optional Ping ping;
}

union PostPingRequest {
  1: optional RootPingRequest root_request;
  2: optional ReplyPingRequest reply_request;
}

# We don't ever throw anything in this file, but it's helpful to mark it as an error type with
# "exception".
exception PingError {
  1: optional string message;
}

union PostPingResponse {
  1: optional PingId pid;
  2: optional PingError error;
}

union GetPingResponse {
  1: optional Ping ping;
  2: optional PingError error;
}

# It is an application-level error if this is empty.
typedef list<entities.CollaborationId> ExplicitCollaborationSet;

struct CollabsForUser {
  1: optional entities.UserId user_id;
}

union CollaborationSpec {
  1: optional ExplicitCollaborationSet explicit_collaboration_set;
  2: optional CollabsForUser collabs_for_user;
}

struct PongsQuery {
  1: optional CollaborationSpec collaboration_spec;
  # The application should use all pings with a WholeRepo location if the result of expanding the
  # HunkGlob is empty, or if no HunkGlobs are provided.
  2: optional repo_backend.HunkGlobs hunk_globs;
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
  # This field may be unset, which means you have to call getPing() with ping_id.
  2: optional Ping ping;
  3: optional Scrutiny scrutiny;
}

typedef list<Pong> PongCollection;

union QueryPongsResponse {
  1: optional PongCollection pongs;
  2: optional PingError error;
}

service PingPong {
  PostPingResponse postPing(1: PostPingRequest request),
  GetPingResponse getPing(1: PingId pid),
  QueryPongsResponse queryPongs(1: PongsQuery),
}
