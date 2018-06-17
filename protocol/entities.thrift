#@namespace scala pingpong.protocol

# A 64-bit signed integer is uniformly used in this file for a key into some abstract backend data
# store.
typedef i64 UserId;

# Should be used to represent a selection of users (including a single user), while UserId is for
# e.g. identifying the author.
typedef i64 GroupId;

typedef i64 PingId;

# E.g. a github pull request, which contains pings and commits.
typedef i64 CollaborationId;
