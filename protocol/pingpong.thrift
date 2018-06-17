#@namespace scala pingpong.protocol.pingpong

# A 64-bit signed integer is uniformly used in this file for a key into some abstract backend data store.
typedef i64 UserId;

struct User {
  1: optional UserId uid;
}

typedef i64 GroupId;

# NB: Should be used to represent users or groups!
struct Group {
  1: optional GroupId gid;
}

struct AnyGroupMember {}

struct AllGroupMembers {}

# This is not a key into anything, only interpreted as a literal unsigned number. Providing a negative value is an application-level error.
typedef i32 SelectionThreshold;

struct AtLeastNGroupMembers {
  1: optional SelectionThreshold threshold;
}

union TargetSelection {
  1: optional AnyGroupMember any;
  2: optional AllGroupMembers all;
  3: optional AtLeastNGroupMembers at_least;
}

struct Target {
  1: optional Group group;
  2: optional TargetSelection;
}

# We do not currently differentiate between different types or urgencies of notifications -- it is not currently clear whether that would be useful.
struct NotifyRequest {
  1: optional Target target;
}

typedef list<NotifyRequest> NotificationSelectors;

struct Ping {
  1: optional User author;
  2: optional NotificationSelectors notifies;
  3: optional string comment_text;
}

typedef i64 LocationId;

struct Location {
  1: optional LocationId lid;
}

struct RootPingRequest {
  1: optional Location location;
  2: optional Ping ping;
}

typedef i64 PingId;

struct ApprovalRequest {
  1: optional PingId pid;
}

typedef list<ApprovalRequest> ApprovalSpecification;

struct ReplyPingRequest {
  1: optional PingId parent;
  2: optional ApprovalSpecification approves;
  3: optional Ping ping;
}

union PostPingRequest {
  1: optional RootPingRequest root_request;
  2: optional ReplyPingRequest reply_request;
}

exception PingSendError {
  1: optional string message;
}

union PostPingResponse {
  1: optional PingId pid;
  2: optional PingSendError error;
}

struct LocationRangeQuery {

}

# "scrutiny" as a term and a concept is my interpretation of what is described in https://blog.janestreet.com/putting-the-i-back-in-ide-towards-a-github-explorer/.
enum Scrutiny {
  # No approval necessary.
  COMMENT = 0,
  # Explicit approval is necessary.
  REVIEW_REQUEST = 1,
}

# A ping, but directed at you.
# TODO: improve description!
struct Pong {
  1: optional Scrutiny scrutiny;
}

struct GetPongsResponse {
}

service PingPong {
  PostPingResponse postPing(1: PostPingRequest request),
}
