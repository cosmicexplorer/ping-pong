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

# This struct maps to a "notification" in a code review system that does not request explicit approval.
# Unless stated otherwise, a "notification" is for a blocking review.
struct NonBlockingNotification {
  1: optional Group group;
}

struct AnyGroupMember {
  1: optional Group group;
}

struct AllGroupMembers {
  1: optional Group group;
}

# This is not a key into anything, only interpreted as a literal unsigned number. Providing a negative value is an application-level error.
typedef i32 SelectionThreshold;

struct AtLeastNGroupMembers {
  1: optional SelectionThreshold threshold;
  2: optional Group group;
}

union TargetSelection {
  1: optional NonBlockingNotification non_blocking;
  2: optional AnyGroupMember any;
  3: optional AllGroupMembers all;
  4: optional AtLeastNGroupMembers at_least;
}

struct NotifyRequest {
  1: optional TargetSelection target;
}

typedef list<NotifyRequest> NotificationSpecification;

# We separate PingContents from the Ping struct so that a service can manipulate the objects in our system without having to understand/model backend-specific dynamics of groups, etc.
struct PingContents {
  1: optional User author;
  2: optional string comment_text;
}

struct Ping {
  1: optional PingContents contents;
  2: optional NotificationSpecification notifies;
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
struct Pong {
  1: optional Location location;
  2: optional PingContents contents;
  3: optional Scrutiny scrutiny;
}

struct GetPongsResponse {
}

service PingPong {
  PostPingResponse postPing(1: PostPingRequest request),
}
