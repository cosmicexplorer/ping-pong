daemon
======

Persistent process that does all communication. Communicates using objects in `protocol`.

Currently a **non**-persistent process, which:
1. [ ] accepts a request over stdin
2. [ ] performs any engine work
2. [ ] performs any backend work
3. [ ] does any frontend stubs
    -  e.g. displays some cli output
4. [x] exits

# TODO
- figure out how to do the interface with frontends
    - json is easy
    - graphql is really neat!
        - [sangria](https://github.com/sangria-graphql/sangria) is a scala interface to it
        - could be highly applicable to the object model here!!!
