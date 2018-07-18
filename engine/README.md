engine
======

**Note:** the `frontend` should handle:
- calculate e.g. status of PR by accumulating the blocking/completing statuses of *ping*s for the PR
- can also have query logic, e.g. "group PRs by repo and rank"

The `engine` would probably be what powers the GraphQL endpoint defined in `protocol/frontend`:
- maybe does caching
