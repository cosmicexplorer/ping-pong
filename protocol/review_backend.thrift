#@namespace scala pingpong.protocol.review_backend

include "entities.thrift"
include "pingpong.thrift"
include "repo_backend.thrift"

# FIXME: the below line doesn't highlight correctly in `scrooge-mode' because it has a string.
# Currently, statuses like "is this PR mergeable?" are left to the frontend, which processes the
# pings from a checkout, according to some set of rules.
struct CheckedOutCollaboration {
  # The checkout's revision should be at the HEAD commit of whatever branch/pull request this is.
  1: optional repo_backend.Checkout checkout;
  2: optional pingpong.PingCollection pings;
}

# It is an application-level error if this is empty.
typedef list<entities.CollaborationId> ExplicitCollaborationSet

struct CollabsForUser {
  1: optional entities.UserId user_id;
}

union CollaborationQuery {
  1: ExplicitCollaborationSet explicit_collaboration_set;
  2: CollabsForUser collabs_for_user;
}

typedef map<entities.CollaborationId, CheckedOutCollaboration> MatchedCollaborations

exception ReviewBackendError {
  1: optional string message;
}

union QueryCollaborationsResponse {
  1: MatchedCollaborations matched_collaborations;
  2: ReviewBackendError error;
}

struct PublishPingsSuccess {}

union PublishPingsResponse {
  1: PublishPingsSuccess success;
  2: ReviewBackendError error;
}

service ReviewBackend {
  # NB: this should also be used to update the ping ids for a collaboration!
  QueryCollaborationsResponse queryCollaborations(1: CollaborationQuery query),
  PublishPingsResponse publishPings(1: pingpong.PingCollection pings),
}
