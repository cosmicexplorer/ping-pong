#@namespace scala pingpong.protocol.review_backend

include "entities.thrift"
include "pingpong.thrift"
include "repo_backend.thrift"

typedef list<entities.PingId> PingIdSet

struct Collaboration {
  1: optional repo_backend.RevisionRange revision_range;
  2: optional repo_backend.Revision head_revision;
  3: optional PingIdSet owned_ping_ids;
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

typedef map<entities.CollaborationId, Collaboration> MatchedCollaborations

exception ReviewBackendError {
  1: optional string message;
}

union QueryCollaborationsResponse {
  1: MatchedCollaborations matched_collaborations;
  2: ReviewBackendError error;
}

typedef list<pingpong.Ping> PingSet

union PublishPingsResponse {
  1: PingIdSet ping_id_set;
  2: ReviewBackendError error;
}

union LookupPingsResponse {
  1: PingSet ping_set;
  2: ReviewBackendError error;
}

struct CollaborationHunk {
  1: optional entities.CollaborationId collaboration_id;
  # The application should use all pings with a WholeRepo location if the result of expanding the
  # HunkGlobs is empty, or if no HunkGlobs are provided.
  2: optional repo_backend.HunkGlobs hunk_globs;
}

# It is an application-level error if this is empty.
typedef list<CollaborationHunk> CollaborationGlobs

union PongsQuery {
  1: CollaborationGlobs collaboration_globs;
  2: CollabsForUser collabs_for_user;
}

typedef list<pingpong.Pong> PongSet

union QueryPongsResponse {
  1: PongSet pong_set;
  2: ReviewBackendError error;
}

service ReviewBackend {
  # NB: this should also be used to update the ping ids for a collaboration!
  QueryCollaborationsResponse queryCollaborations(1: CollaborationQuery query),
  PublishPingsResponse publishPings(1: PingSet pings),
  LookupPingsResponse lookupPings(1: PingIdSet ping_ids),
  # FIXME: the below line doesn't highlight correctly in `scrooge-mode' because it has a string.
  # Currently, statuses like "is this PR mergeable?" are left to the frontend, which processes the
  # result of this method.
  QueryPongsResponse queryPongs(1: PongsQuery query),
}
