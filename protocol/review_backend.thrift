#@namespace scala pingpong.protocol

include "entities.thrift"
include "pingpong.thrift"
include "repo_backend.thrift"

typedef list<entities.PingId> PingIdSet;

struct Collaboration {
  1: optional repo_backend.RevisionRange revision_range;
  2: optional repo_backend.Revision head_revision;
  3: optional PingIdSet owned_ping_ids;
}

# It is an application-level error if this is empty.
typedef list<entities.CollaborationId> ExplicitCollaborationSet;

struct CollabsForUser {
  1: optional entities.UserId user_id;
}

union CollaborationQuery {
  1: optional ExplicitCollaborationSet explicit_collaboration_set;
  2: optional CollabsForUser collabs_for_user;
}

typedef map<entities.CollaborationId, Collaboration> MatchedCollaborations;

exception ReviewBackendError {
  1: optional string message;
}

union QueryCollaborationsResponse {
  1: optional MatchedCollaborations matched_collaborations;
  2: optional ReviewBackendError error;
}

typedef list<ping.Ping> PingSet;

union PublishPingsResponse {
  1: optional PingIdSet ping_id_set;
  2: optional ReviewBackendError error;
}

union LookupPingsResponse {
  1: optional PingSet ping_set;
  2: optional ReviewBackendError error;
}

struct CollaborationHunk {
  1: optional entities.CollaborationId collaboration_id;
  # The application should use all pings with a WholeRepo location if the result of expanding the
  # HunkGlobs is empty, or if no HunkGlobs are provided.
  2: optional repo_backend.HunkGlobs hunk_globs;
}

# It is an application-level error if this is empty.
typedef list<CollaborationHunk> CollaborationGlobs;

union PongsQuery {
  1: optional CollaborationGlobs collaboration_globs;
  2: optional CollabsForUser collabs_for_user;
}

typedef list<pong.Pong> PongSet;

union QueryPongsResponse {
  1: optional PongSet pong_set;
  2: optional ReviewBackendError error;
}

service ReviewBackend {
  # NB: this should also be used to update the ping ids for a collaboration!
  QueryCollaborationsResponse queryCollaborations(1: CollaborationQuery),
  PublishPingsResponse publishPings(1: PingSet),
  LookupPingsResponse lookupPings(1: PingIdSet),
  QueryPongsResponse queryPongs(1: PongsQuery),
}
