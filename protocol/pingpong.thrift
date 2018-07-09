#@namespace scala pingpong.protocol.pingpong

include "entities.thrift"
include "notifications.thrift"

struct RepoFile {
  # Path to a file relative to the repo root.
  1: optional string file_relative_path;
}

# If this is <= 0, that is an application-level error.
typedef i32 LineNumber

struct LineRangeForFile {
  1: optional LineNumber beginning;
  2: optional LineNumber ending;
}

struct FileWithRange {
  1: optional RepoFile file;
  # If not provided, the whole file is assumed.
  # This will be interpreted as just the beginning of the specified range, if the review backend
  # only supports tracking comments at a single location, instead of spanning multiple lines (all of
  # them, except our git notes backend).
  2: optional LineRangeForFile line_range_in_file;
}

# Represents some contiguous selection of the contents of some file in the underlying repo at some
# specific revision.
struct Hunk {
  1: optional FileWithRange file_with_range;
}

struct PingLocation {
  1: optional list<Hunk> hunk_collection;
}

struct RegionComment {
  1: optional PingLocation ping_location;
}

# E.g. a comment on the pull request itself, not tied to a particular line.
struct ThreadComment {}

struct Reply {
  # This should be local to the containing `PingCollection`. Applications can decide
  # what to do if it is not contained in there (probably error out).
  # Applications should follow the thread of `Reply` pings through `parent` ids until reaching a
  # `RegionComment` or `ThreadComment`.
  1: optional entities.PingId parent;
}

union PingSource {
  1: RegionComment region_comment;
  2: ThreadComment thread_comment;
  3: Reply reply;
}

struct Ping {
  1: optional PingSource source;
  2: optional notifications.TargetSpecification notifies;
  # NB: It should be enforced at the application level some approval permissions model,
  # e.g. "approvals must be done in a ping that replies (directly?) to the ping being approved".
  3: optional notifications.ApprovalSpecification approves;
  4: optional entities.UserId author;
  # This is just the text a human wrote -- individual backends may add additional text, e.g. adding
  # a github @user in the comment text for all the users that need to be notified. Backends should
  # ensure they return exactly the comment_text of the Ping they received, or update the
  # comment_text of the ping at that id.
  5: optional string body;
}
