#@namespace scala pingpong.protocol

# If this is <= 0, that is an application-level error.
typedef i32 LineNumber;

struct LineRangeInFile {
  1: optional LineNumber begin;
  2: optional LineNumber end;
}

# Represents some contiguous selection of the contents of some file in the underlying repo at some
# specific revision.
struct Hunk {
  # This is a backend-specific string for a specific ref in the repo (e.g. commit).
  1: optional string backend_revision;
  # Path to a file relative to the repo root.
  1: optional string file_relative_path;
  # If not provided, the whole file is assumed.
  2: optional LineRangeInFile line_range_in_file;
}

typedef list<Hunk> HunkCollection;

# Used to represent comments not tied to a specific file or set of files (e.g. github PR comments in
# the main thread).
struct WholeRepo {}

union Location {
  1: optional HunkCollection hunk_collection;
  2: optional WholeRepo whole_repo;
}
