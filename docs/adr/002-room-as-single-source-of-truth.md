# ADR-002: Room as the single source of truth for movie lists

Status: accepted

## Context

Movie lists come from TMDB and must work offline, page smoothly and stay consistent when the
same movie appears in several lists. An earlier design wrote lists from two places (a
`networkBoundResource` read path and the `RemoteMediator`) into one table keyed by movie id,
which let one list overwrite another and let search results leak into categories.

## Decision

- The UI only reads from Room, through `Pager` with a `RemoteMediator`. The mediators are the
  only writers of list data.
- Normalised schema: `movies` holds one canonical row per movie. List membership is stored in
  `movie_category(category, movie_id, position)` and `search_result(query, movie_id, position)`,
  both with a cascading foreign key to `movies`. `remote_keys` stores pagination state per list.
- Movies are written with an upsert, never `REPLACE`, because `REPLACE` deletes the row and
  would cascade into the list tables.
- A page write (clearing on refresh, movies, memberships, remote key) is one transaction in
  `LocalDataSource`. The network fetch happens before the transaction, so a failed refresh keeps
  the cached list.
- Movies that no list references any more are deleted when a list is refreshed. Search results
  older than 24 hours are removed on the next search refresh.
- Movie details use `networkBoundResource`, since they are a single record, not a list.
- Pull-to-refresh calls `LazyPagingItems.refresh()`, which re-runs the mediator.

## Consequences

- Offline behaviour and list consistency come from one mechanism.
- Schema changes need a Room migration and a schema test; the app was never released, so the
  schema was reset to version 1 when this design was introduced.
- Reads need a join; this is covered by indices on `(category, position)` and `(query, position)`.
