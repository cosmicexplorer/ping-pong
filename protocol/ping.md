ping
====

- author: User(uid)
    - *uid* is a user id
- comment_text: String
- notifies: [enum { User(uid) | Group(name, selection: enum { Any | All | AtLeast(N: Int) }) }]
    - *name* is probably a string
- scrutiny: enum { Comment | ReviewRequest }
- location: enum { DiffRegion(...) | Parent(pid) }
    - *pid* is a ping id
