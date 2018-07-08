#@namespace scala pingpong.protocol.review_backend

include "pingpong.thrift"
include "repo_backend.thrift"

struct CollaborationId {
  1: optional string cid;
}

# FIXME: the below line doesn't highlight correctly in `scrooge-mode' because it has a string.
# Currently, statuses like "is this PR mergeable?" are left to the frontend, which processes the
# pings from a checkout, according to some set of rules.
struct Collaboration {
  # The checkout's revision should be at the HEAD commit of whatever branch/pull request this is.
  1: optional repo_backend.Checkout checkout;
  2: optional pingpong.PingCollection pings;
}

# This may be empty. Applications should probably throw an error if that happens, because it's
# likely erroneous.z
typedef list<CollaborationId> CollaborationIdSet

struct CollaborationQuery {
  1: optional CollaborationIdSet collaboration_ids;
}

# NB: `CollaborationId` *could* be global (like a git remote), and that would be very neat
# (we could parse it to determine which backend to route it to).
# NB: `CollaborationId` instances need not be "global" in any sense -- we just need the
# `collaboration_id` in `PublishPingsRequest` (or the elements of `ExplicitCollaborationSet`) to
# match something that was returned to the client in this map.
typedef map<CollaborationId, Collaboration> CollaborationMap

struct MatchedCollaborations {
  1: optional CollaborationMap collaborations;
}

exception ReviewBackendError {
  1: optional string message;
}

union QueryCollaborationsResponse {
  1: MatchedCollaborations matched_collaborations;
  2: ReviewBackendError error;
}

typedef list<pingpong.Ping> PublishPingSet

struct PublishPingsRequest {
  1: optional CollaborationId collaboration_id;
  2: optional PublishPingSet pings_to_publish;
}

union PublishPingsResponse {
  1: pingpong.PingCollection published_pings;
  2: ReviewBackendError error;
}

service ReviewBackend {
  QueryCollaborationsResponse queryCollaborations(1: CollaborationQuery query),
  PublishPingsResponse publishPings(1: PublishPingsRequest request),
}
