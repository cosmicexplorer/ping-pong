ping
====

- author: User(uid)
    - *uid* is a user id
- comment_text: String
- notifies: [enum { User(uid) | Group(name, selection: enum { Any | All | AtLeast(N: Int) }) }]
    - *name* is probably a string
- scrutiny: enum { Comment | ReviewRequest }
    - a *Comment* is "resolved" once viewed (but can be marked unread again)
    - a *ReviewRequest* requires an explicit **positive** (*only* positive) confirmation from a person or group
    - these enum values are ranked in terms of importance, which is useful to the `engine`
- approves: [Request(pid)]
    - *pid* is a ping id
- location: enum { DiffRegion(...) | Parent(pid) }
