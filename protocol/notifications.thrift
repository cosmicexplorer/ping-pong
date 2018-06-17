#@namespace scala pingpong.protocol

include "entities.thrift"

# This struct maps to a "notification" in a code review system that does not request explicit
# approval.
# Unless stated otherwise, a "notification" is for a blocking review.
struct NonBlockingNotification {}

struct AnyMember {}

struct AllMembers {}

# This is not a key into anything, only interpreted as a literal unsigned number. Providing a
# negative value is an application-level error.
typedef i32 SelectionThreshold;

struct AtLeastNMembers {
  1: optional SelectionThreshold threshold;
}

union TargetSelection {
  1: optional NonBlockingNotification non_blocking;
  2: optional AnyMember any;
  3: optional AllMembers all;
  4: optional AtLeastNMembers at_least;
}

typedef list<TargetSelection> TargetSpecification;

struct ApprovalRequest {
  1: optional entities.PingId pid;
}

typedef list<ApprovalRequest> ApprovalSpecification;
