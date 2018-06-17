#@namespace scala pingpong.protocol

struct File {
  # Path to a file relative to the repo root.
  1: optional string file_relative_path;
}

# If this is <= 0, that is an application-level error.
typedef i32 LineNumber;

struct LineRangeForFile {
  1: optional LineNumber beginning;
  2: optional LineNumber ending;
}

struct FileWithRange {
  1: optional File file;
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

typedef list<Hunk> HunkCollection;

# Used to represent comments not tied to a specific file or set of files (e.g. github PR comments in
# the main thread).
struct WholeRepo {}

union PingLocation {
  1: optional HunkCollection hunk_collection;
  2: optional WholeRepo whole_repo;
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

typedef list<PathGlob> PathGlobs;

struct PathGlobsWithinRevisionRange {
  # The application should use all the commits in the collaboration if this is not provided.
  1: optional RevisionRange revision_range;
  2: optional PathGlobs path_globs;
}

# Specifies a range which can resolve to zero or more Hunk objects.
union HunkGlob {
  1: optional PathGlobsWithinRevisionRange path_revision_range_globs;
  # This is an extremely narrow selector.
  2: optional Hunk hunk;
}

typedef list<HunkGlob> HunkGlobs;

struct GetSandboxGlobsRequest {
  1: optional Revision revision;
  2: optional PathGlobs path_globs;
}

struct Sandbox {
  1: optional string sandbox_root_absolute_path;
}

typedef list<File> Fileset;

struct SandboxWithExpandedGlobs {
  1: optional Sandbox sandbox;
  2: optional Fileset fileset;
}

exception RepoBackendError {
  1: optional string message;
}

union GetSandboxGlobsResponse {
  1: optional SandboxWithExpandedGlobs sandbox_expanded_globs;
  2: optional RepoBackendError error;
}

service RepoBackend {
  GetSandboxGlobsResponse getSandboxGlobs(1: GetSandboxGlobsRequest request),
}
