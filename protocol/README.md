protocol
========

- what is a *ping*? a serializable object processed and interpreted by the `engine`
    - e.g. a review request, a comment, a reply
- also, `daemon<->frontend` communication should be defined here in a subdir eventualy

# TODO

## ping

- some way to represent e.g. typo fixes that can be **positively** accepted (**NOT** rejected!) to perform the fix in a location tracked by `repo-backends`, and perform e.g. a commit.
    - instead of rejecting it, just make another ping that approves it!

# Resources

- [Thrift: The Missing Guide](https://diwakergupta.github.io/thrift-missing-guide)
