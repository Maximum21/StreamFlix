# StreamFlix: Progress Tracker

> Status, next action and decisions for the remediation work. Read together with `docs/PLAN.md` (what and why) and `docs/AUDIT.md` (findings).
> Updated after every completed task and at the end of each working session.

## Working agreement
- Each change is implemented step by step, with the reasoning and the pattern behind it written down in the plan.
- Goal: a **Staff-level Android portfolio** project and a **Play Store release**.
- Backend: TMDB public API. Firebase scope: Auth + Firestore, Crashlytics + Analytics + Performance, Remote Config + App Distribution, Cloud Messaging.
- The repository is **public**. No secrets, `google-services.json`, or keystores may ever be committed.
- The `:download` module and Downloads tab are **removed** for v1.
- Branch names: `type/short-description` (for example `chore/repo-baseline`, `fix/search-paging-cache`). Conventional Commits. Work merges into `main` through a pull request with CI green.

## Status at a glance

| Phase | Name | Status |
|---|---|---|
| 0 | Secret hygiene and repo baseline | **Done** (pending PR merge) |
| 1 | Make CI honest | Next |
| 2 | Data integrity | Not started |
| 3 | Architecture cleanup | Not started |
| 4 | App shell and stability | Not started |
| 5 | Security and release plumbing | Not started |
| 6 | Tests to the coverage gate | Not started |
| 7 | Product features | Not started |
| 8 | Polish, release and portfolio | Not started |

**NEXT ACTION:** Phase 1, step 1.1: remove the `:download` module (module folder, `Screen.Downloads`, bottom-nav entry, unused permissions). Branch `chore/remove-download-module`.

## Repo snapshot (as of 2026-10-07, before Phase 0.2)
- Branch: `work_in_progress` (pushed to `origin/work_in_progress`), `main` also exists. 2 commits: `f9d6239`, `5b08741`. About 103 uncommitted entries.
- Tracked but must not be: `secrets.properties`, `app/google-services.json` (in commit `f9d6239`).
- Untracked but must be tracked: `.gitignore`, `.github/workflows/ci.yml`, `gradlew`, `gradlew.bat`, `config/detekt/`, `database/schemas/`, per-module `.gitignore`, `docs/`.
- Last verified build: `./gradlew --continue compileDebugKotlin testDebugUnitTest detekt ktlintCheck lintDebug` → **BUILD FAILED**. `data`, `domain`, `home` tests don't compile. 368 detekt findings, 57 ktlint tasks fail, lint 0 errors / 116 warnings.
- Local `secrets.properties` holds the **rotated** token (owner-confirmed).

## Task checklist

Legend: `[ ]` todo, `[~]` in progress, `[x]` done. "(confirmed manually)" = cannot be verified from the repository.

### Phase 0: Secret hygiene and repo baseline
- [x] 0.1 Rotate TMDB v3 key + v4 token; restrict Firebase API key; change burned sign-up password (confirmed manually, 2026-10-07)
- [x] 0.2 Create branch; extend `.gitignore`; commit `.gitignore` alone (commit 73f6ddd, 2026-10-13)
- [x] 0.3 Untrack secrets; add `secrets.properties.example` (commit f93a03d, 2026-10-13)
- [x] 0.4 History reset: single clean commit 2c834fb force-pushed to `main`; remote `work_in_progress` deleted; old local objects purged (2026-10-13). Backup of old repo at ../StreamFlix-backup (delete when no longer needed)
- [x] 0.5 Baseline files committed (gradlew, config/detekt, CI workflow, schemas, module .gitignores, build-logic, docs, LICENSE, README)
- [x] 0.6 Build works without secrets: env fallback and quote handling in `network/build.gradle.kts`, JDK 17 toolchain in the library convention plugin (fresh clone builds `:network` and `:domain`)
- [x] 0.7 Hardcoded credentials / pre-ticked terms removed from `SignUpContract.kt` (included in 2c834fb)
- [x] **Gate 0** verified (2026-10-13): rotated keys, no secrets in tree or history, fresh clone builds without secrets. Full `:app` assembly still needs `google-services.json` (injected in CI, Phase 1)

### Phase 1: Make CI honest
- [ ] 1.1 Delete `:download` module, `Screen.Downloads`, nav item, unused permissions
- [ ] 1.2 Delete template tests; remove stray `data/src/main/.../MovieMapperTest.kt`
- [ ] 1.3 Fix the 3 stale tests so `testDebugUnitTest` compiles and passes
- [ ] 1.4 `ktlintFormat` + manual fixes (separate commits)
- [ ] 1.5 Detekt baseline, `maxIssues: 0`, re-enable cancellation and complexity rules
- [ ] 1.6 `:testing` module; test deps in convention plugin; remove Mockito entries
- [ ] 1.7 Kover via convention plugin (report only)
- [ ] 1.8 Rewrite `ci.yml` (PR + main, concurrency, cache, artifacts, secrets injection, wrapper validation)
- [ ] 1.9 Gitleaks, Dependabot, PR template, branch protection
- [ ] 1.10 Gradle performance flags; remove `force()` block
- [ ] **Gate 1**

### Phase 2: Data integrity
- [ ] 2.1 Tests first (in-memory Room, `paging-testing`) reproducing B4/B5
- [ ] 2.2 New schema: `movie_category` join table + indices
- [ ] 2.3 Search: `search_result(query, movieId, position)`, delete-by-query on REFRESH
- [ ] 2.4 Single writer (remove `networkBoundResource` list path)
- [ ] 2.5 `withTransaction` in mediators
- [ ] 2.6 Rethrow `CancellationException` (safeApiCall, mediators, networkBoundResource)
- [ ] 2.7 One `MovieDto.toEntity` mapper; one page-size constant
- [ ] 2.8 Reset DB to v1; delete `MigrationsUtil.kt`; move `DATABASE_VERSION` to `:database`; migration-test harness
- [ ] 2.9 Retry interceptor (cap, jitter, 5xx/IOException on idempotent) + MockWebServer tests
- [ ] 2.10 Delete dead code (old PagingSources, ErrorInterceptor decision, PaginatedResult, rate-limit constant)
- [ ] **Gate 2**

### Phase 3: Architecture cleanup
- [ ] 3.1 `cachedIn` to ViewModel; use cases return `Flow<PagingData>`
- [ ] 3.2 Shared grid to `:ui`; remove `search → home`
- [ ] 3.3 Dispatcher qualifiers to `:common`; inject everywhere
- [ ] 3.4 Remove Retrofit from `:common`
- [ ] 3.5 Repository API cleanup; split or document `MovieRepositoryImpl`; delete `discoverMovies`
- [ ] 3.6 `AuthDataSource` / `ProfileDataSource` ports
- [ ] 3.7 Error model + `UiText`; explicit Firebase exception mapping
- [ ] 3.8 Static category enum for tabs
- [ ] 3.9 Convention plugins everywhere; trim module dependencies; clean catalog
- [ ] 3.10 Rename packages / files
- [ ] 3.11 ADR-001 (module structure), ADR-002 (Room SSOT)
- [ ] **Gate 3**

### Phase 4: App shell and stability
- [ ] 4.1 Route-derived bottom bar; register Profile/Settings destinations
- [ ] 4.2 Runtime auth guard; Login → ProfileSelection → Home
- [ ] 4.3 Remove manifest `configChanges`; fix state restoration
- [ ] 4.4 Crash fixes (trailer selection, `TODO()`, stable keys)
- [ ] 4.5 Player lifecycle; trailers only; remove dead Media3 path; ADR-003
- [ ] 4.6 Remove debug logs; Timber only
- [ ] 4.7 Dark splash theme, `SystemBarStyle`, insets once
- [ ] 4.8 Crashlytics + Performance + `CrashlyticsTree`
- [ ] **Gate 4**

### Phase 5: Security and release plumbing
- [ ] 5.1 `firestore.rules`, `firebase.json`, emulator tests in CI
- [ ] 5.2 App Check
- [ ] 5.3 Backup rules (`allowBackup=false`)
- [ ] 5.4 Network security config + pinning + ADR
- [ ] 5.5 R8 rules + verified release build
- [ ] 5.6 Profile hardening (auto-ids, transactions, clear on sign-out)
- [ ] 5.7 Auth fixes (U-1, U-2, U-5, U-6, S-9, autofill, `FLAG_SECURE`)
- [ ] 5.8 Signing config + `bundleRelease`
- [ ] 5.9 Dependency upgrades (one group per commit)
- [ ] 5.10 OSV-Scanner clean; `docs/SECURITY.md`
- [ ] **Gate 5**

### Phase 6: Tests to coverage gate  (interleaved with phases 2-5)
- [ ] 6.1 Validators and use cases
- [ ] 6.2 Auth ViewModels
- [ ] 6.3 Network (MockWebServer)
- [ ] 6.4 Repositories and mediators
- [ ] 6.5 Remaining ViewModels
- [ ] 6.6 Compose screens (Robolectric) and navigation
- [ ] 6.7 Hilt smoke test + one instrumented flow
- [ ] 6.8 Enable Kover gate (60% overall, 80% domain/data) + badge
- [ ] **Gate 6**

### Phase 7: Product features
- [ ] 7.1 Home hero + rows
- [ ] 7.2 Detail enrichment
- [ ] 7.3 My List (Firestore)
- [ ] 7.4 Continue watching
- [ ] 7.5 Search history / filters
- [ ] 7.6 Settings incl. account deletion
- [ ] 7.7 Remote Config, Analytics + consent, FCM
- [ ] 7.8 Deep links
- [ ] **Gate 7**

### Phase 8: Polish, release, portfolio
- [ ] 8.1 Design system, light/dark/dynamic colour, previews, stability
- [ ] 8.2 Accessibility, i18n, RTL
- [ ] 8.3 Adaptive layouts
- [ ] 8.4 Coil config, baseline profile, Macrobenchmark
- [ ] 8.5 CD: signed AAB → App Distribution → Play internal
- [ ] 8.6 Play compliance (TMDB attribution, privacy policy, Data Safety, rename app/icon)
- [ ] 8.7 README, ADRs, diagrams, CONTRIBUTING, CHANGELOG
- [ ] **Gate 8** (Definition of Done, AUDIT §9)

## Decision log
| Date | Decision | Why |
|---|---|---|
| 2026-10-07 | Audit first, then implement phase by phase | Every decision is documented with its rationale |
| 2026-10-07 | Repo will be public | Portfolio. Forces strict secret hygiene |
| 2026-10-07 | Delete `:download` for v1 | Uncompilable, and TMDB has no downloadable content source |
| 2026-10-07 | Trailers-only player for v1 (planned, ADR-003) | TMDB provides metadata and YouTube keys, not streams |
| 2026-10-07 | Reset git history rather than only untracking | Two WIP commits, secrets inside, nothing worth preserving |

## Open questions / unverified
- Is the repo already public, and was it cloned? (Rotation done either way.)
- Firebase console: current Firestore rules, App Check, email-enumeration protection (owner to check in Phase 5).
- No device run of the app has been done; no CVE scan yet (Phase 5.10).
- App name and icon: "StreamFlix" resembles Netflix. Decide in Phase 8.6.

## Session log
| Date | Summary | Ended at |
|---|---|---|
| 2026-10-07 | Full audit (verified build run + 5 code audits) → `docs/AUDIT.md`; plan → `docs/PLAN.md`; tracker created. Credentials rotated (0.1). | Next: 0.2 |
