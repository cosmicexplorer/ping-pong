#@namespace scala pingpong.protocol.repo_backend

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
  2: optional LineRangeForFile line_range_in_file;
}

struct Revision {
  # This is a backend-specific string for a specific ref in the repo (e.g. commit).
  1: optional string backend_revision_spec;
}

# Represents some contiguous selection of the contents of some file in the underlying repo at some
# specific revision.
struct Hunk {
  1: optional Revision revision;
  2: optional FileWithRange file_with_range;
}

typedef list<Hunk> HunkCollection

# Used to represent comments not tied to a specific file or set of files (e.g. github PR comments in
# the main thread).
struct WholeRepo {}

union PingLocation {
  1: HunkCollection hunk_collection;
  2: WholeRepo whole_repo;
}

# E.g. a git commit range.
struct RevisionRange {
  # This is going to be different across different version control systems, so we leave it as a
  # string for now.
  1: optional string backend_revision_range_spec;
}

struct PathGlob {
  # This is relative to the repo root.
  1: optional string relative_glob;
}

typedef list<PathGlob> PathGlobs

struct PathGlobsWithinRevisionRange {
  # The application should use all the commits in the collaboration if this is not provided.
  1: optional RevisionRange revision_range;
  2: optional PathGlobs path_globs;
}

# Specifies a range which can resolve to zero or more Hunk objects.
union HunkGlob {
  1: PathGlobsWithinRevisionRange path_revision_range_globs;
  # This is an extremely narrow selector.
  2: Hunk hunk;
}

typedef list<HunkGlob> HunkGlobs

struct RepoLocation {
  # E.g. a local or remote git repo.
  1: optional string backend_location_spec;
}

struct CheckoutRequest {
  1: optional RepoLocation source;
  2: optional Revision revision;
}

struct CheckoutLocation {
  # In the future, this may represent an arbitrary address, not just a file path.
  1: optional string sandbox_root_absolute_path;
}

struct Checkout {
  1: optional CheckoutLocation checkout_location;
  2: optional RepoLocation source;
  3: optional Revision revision;
}

typedef list<RepoFile> RepoFileset

exception RepoBackendError {
  1: optional string message;
}

union CheckoutResponse {
  1: Checkout completed;
  2: RepoBackendError error;
}

service RepoBackend {
  CheckoutResponse getCheckout(1: CheckoutRequest request),
}
