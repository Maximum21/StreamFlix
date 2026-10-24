# ADR-001: Module structure and dependency rule

Status: accepted

## Context

The app is split into Gradle modules so that features build independently, layers stay testable
and the dependency direction is enforced by the build instead of by convention.

## Decision

| Module | Role |
|---|---|
| `:app` | Application shell: navigation graph, Hilt entry point, wiring of features |
| `:auth` `:home` `:search` `:detail` `:player` `:profile` `:settings` `:mylist` | Feature modules (screens, ViewModels, MVI contracts) |
| `:ui` | Shared Compose components, theme, `UiText` and error-to-text mapping |
| `:domain` | Repository interfaces and use cases |
| `:data` | Repository implementations, mediators, data-source ports and their Firebase/DataStore implementations |
| `:network` | Retrofit service, interceptors, `safeApiCall` |
| `:database` | Room database, DAOs, entities, `LocalDataSource` |
| `:model` | Plain Kotlin domain models |
| `:common` | `Result`, `ErrorType`, constants, dispatcher qualifiers |
| `:testing` | Shared test fixtures and rules, `testImplementation` only |

Rules, enforced by the `checkModuleGraph` Gradle task (part of `check`):

1. Dependencies point towards the core. `:model` and `:common` depend on nothing in the app.
2. Feature modules never depend on each other, nor on `:data`, `:network` or `:database`.
   Anything two features need goes to `:ui` (components) or `:domain` (logic).
3. `:domain` has no dependency on `:data`, `:network`, `:database` or `:ui`.
4. `:ui` has no dependency on features or on the data layers.
5. Only `:app` sees every module, because it wires Hilt.

`:domain` uses `androidx.paging` types (`PagingData`) in repository signatures. This is a
deliberate exception: Paging's common artifact has no Android UI dependency, and mapping to a
custom page type would duplicate it without benefit.

## Consequences

- A new feature is added as a module and can only reach shared code through `:ui`/`:domain`.
- Violations fail the build locally and in CI.
- The cost is more modules and some indirection; this is accepted for build isolation and testability.
