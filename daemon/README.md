daemon
======

Persistent process that does all communication. Communicates using objects in `protocol`.

Currently a **non**-persistent process, which:
1. accepts a request over stdin
2. performs any engine work
2. performs any backend work
3. does any frontend stubs
    -  e.g. displays some cli output
4. exits
