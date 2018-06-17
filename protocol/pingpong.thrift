#@namespace scala pingpong.protocol

include "entities.thrift"
include "location.thrift"

# This struct maps to a "notification" in a code review system that does not request explicit
# approval.
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

# This is not a key into anything, only interpreted as a literal unsigned number. Providing a
# negative value is an application-level error.
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

struct Ping {
  1: optional location.Location location;
  2: optional NotificationSpecification notifies;
  3: optional User author;
  # This is just the text a human wrote -- individual backends may add additional text, e.g. adding
  # a github @user in the comment text for all the users that need to be notified. Backends should
  # ensure they return exactly the comment_text of the Ping they received, or update the
  # comment_text of the ping at that id.
  4: optional string comment_text;
}

struct ParentPing {
  1: optional PingId pid;
}

struct RootPingRequest {
  1: optional Hunk pertinent_range;
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

# We don't ever throw anything in this file, but it's helpful to mark it as an error type with
# "exception".
exception PostPingError {
  1: optional string message;
}

union PostPingResponse {
  1: optional PingId pid;
  2: optional PostPingError error;
}

exception GetPingError {
  1: optional string message;
}

union GetPingResponse {
  1: optional Ping ping;
  2: optional GetPingError error;
}

typedef i64 CollaborationId;

# E.g. a github pull request.
struct Collaboration {
  1: optional CollaborationId cid;
}

# E.g. a git commit range.
struct RevisionRange {
  # This is going to be different across different version control systems, so we leave it as a
  # string for now.
  1: optional string backend_range_spec;
}

# Specifies a range which can resolve to zero or more Hunk objects. This will be able to
# specify a selection of files at least. This will not be backend-specific and wil not be limited to
# file paths.
struct HunkFilter {
  1: optional
}

typedef list<Collaboration> CollaborationSet;

struct PongQuery {
  # Search for pings made in these collaborations.
  1: optional CollaborationSet collaboration_set;
  1: optional HunkFilter pertinent_range_filter;
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
  1: optional PingId pid;
  2: optional Scrutiny scrutiny;
}

typedef list<Pong> PongCollection;

struct GetPongsResponse {
  1: optional PongCollection pongs;
}

service PingPong {
  PostPingResponse postPing(1: PostPingRequest request),
  GetPingResponse getPing(1: PingId pid),
}
