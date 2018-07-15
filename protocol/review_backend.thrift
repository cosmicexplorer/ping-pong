#@namespace scala pingpong.protocol.review_backend

include "entities.thrift"
include "pingpong.thrift"
include "repo_backend.thrift"

struct CollaborationId {
  1: optional string cid;
}

# This represents a ping which has been successfully attached to a specific revision --
# `PinPingRequest` is consumed in a review backend to produce this.
struct PinnedPing {
  1: optional pingpong.Ping ping;
  2: optional repo_backend.Revision revision;
}

# NB: This is for reading/writing by clients, and *all* `PingId` instances are local to this map!
# TODO: could just use a list here, and make ping ids i32s which index into that list -- that's a
# perf opportunity for later.
struct PingCollection {
  1: optional map<entities.PingId, PinnedPing> ping_map;
}

# FIXME: the below line doesn't highlight correctly in `scrooge-mode' because it has a string.
# Currently, statuses like "is this PR mergeable?" are left to the frontend, which processes the
# pings from a checkout, according to some set of rules.
struct Collaboration {
  # The checkout's revision should be at the HEAD commit of whatever branch/pull request this is.
  1: optional repo_backend.Checkout checkout;
  2: optional PingCollection pings;
}

# This may be empty. Applications should probably throw an error if that happens, because it's
# likely erroneous.
struct CollaborationQuery {
  1: optional list<CollaborationId> collaboration_ids;
}

# NB: `CollaborationId` *could* be global (like a git remote), and that would be very neat
# (we could parse it to determine which backend to route it to).
# NB: `CollaborationId` instances need not be "global" in any sense -- we just need the
# `collaboration_id` in `PublishPingsRequest` (or the elements of `ExplicitCollaborationSet`) to
# match something that was returned to the client in this map.
struct MatchedCollaborations {
  1: optional map<CollaborationId, Collaboration> collaborations;
}

exception ReviewBackendError {
  1: optional string message;
}

union QueryCollaborationsResponse {
  1: MatchedCollaborations matched_collaborations;
  2: ReviewBackendError error;
}

struct PinPingRequest {
  1: optional pingpong.Ping ping;
  2: optional repo_backend.Revision revision;
}

struct PublishPingsRequest {
  1: optional CollaborationId collaboration_id;
  2: optional list<PinPingRequest> pings_to_pin;
}

union PublishPingsResponse {
  1: PingCollection published_pings;
  2: ReviewBackendError error;
}

service ReviewBackend {
  QueryCollaborationsResponse queryCollaborations(1: CollaborationQuery query),
  PublishPingsResponse publishPings(1: PublishPingsRequest request),
}
