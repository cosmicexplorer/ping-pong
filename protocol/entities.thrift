#@namespace scala pingpong.protocol.entities

# A string is uniformly used in this file for a key into some abstract backend data store.
typedef string UserId

# Should be used to represent a selection of users (including a single user), while UserId is for
# e.g. identifying the author.
typedef string GroupId

typedef string PingId

# E.g. a github pull request, which contains pings and commits.
typedef string CollaborationId

# FIXME: thrift/scrooge doesn't generate a namespace for this file unless we have at least one
# struct or something besides a typedef.
struct _AStruct {}
