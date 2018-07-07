#@namespace scala pingpong.protocol.entities

# These should all be entities that can be read and written by a frontend (e.g. an emacs
# plugin). Accordingly, they should be descriptive when stringified, yet unique (???).

struct UserId {
  1: optional string uid;
}

# Should be used to represent a selection of users (including a single user), while UserId is for
# e.g. identifying the author.
struct GroupId {
  1: optional string gid;
}

struct PingId {
  1: optional string pid;
}
