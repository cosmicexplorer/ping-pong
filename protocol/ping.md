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
        - this transitively blocks the review from being mergeable
    - these enum values are ranked in terms of importance, which is useful to the `engine`
- approves: [Request(pid)]
    - *pid* is a ping id
- location: enum { DiffRegion(...) | Parent(pid) }

# TODO

- some way to represent e.g. typo fixes that can be **positively** accepted (**NOT** rejected!) to perform the fix in a location tracked by `repo-backends`, and perform e.g. a commit.
    - instead of rejecting it, just make another ping that approves it!
