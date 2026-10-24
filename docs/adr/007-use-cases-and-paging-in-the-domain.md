# ADR-007: Use cases as the only entry point, and Paging types in the domain

Status: accepted

## Context

A design review of the project raised two things that are deliberate rather than accidental.
Many use cases only forward a call to a repository, and the domain module exposes
`androidx.paging.PagingData` from its repository interfaces.

## Decision

- **Use cases are the only thing ViewModels depend on**, including the ones that just forward to a
  repository. The layer costs a few lines each, keeps ViewModels free of repository details, gives
  one place to add a rule later (as `SaveProfileUseCase` and `DeleteAccountUseCase` already have),
  and makes ViewModels easy to test with a single mock per action. If the pass-through ones are ever
  felt to be noise, they can be replaced by injecting the repository interface, one at a time.
- **`PagingData` stays in the domain interfaces.** Paging 3's `Pager` and `RemoteMediator` need to
  sit next to the data sources, and hiding `PagingData` behind a custom page type would mean mapping
  every page twice for no behavioural gain. Only `paging-common` types are used, never Android
  classes. The same trade-off is made by Google's own architecture samples.
- Empty results for inputs that should not hit the network (a short search query) are produced
  through one helper, `noResults()`, which reports a finished load so lists show their empty
  state instead of a spinner.

## Consequences

- The domain module has a dependency on `paging-common`. It must not grow other Android
  dependencies.
- A future non-Android client of the domain would need an adapter for `PagingData`.
