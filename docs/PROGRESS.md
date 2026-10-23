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
| 1 | Make CI honest | **Done locally** (pending PR and green CI, Gate 1) |
| 2 | Data integrity | **Done locally** (device check of paging pending) |
| 3 | Architecture cleanup | **Done locally** |
| 4 | App shell and stability | **Done locally** (device smoke test pending) |
| 5 | Security and release plumbing | **Done locally** (console steps, keystore and release smoke test pending) |
| 6 | Tests to the coverage gate | **Done locally** (pending green CI, Gate 6) |
| 7 | Product features | **Done locally** (console steps and device pass pending) |
| 8 | Polish, release and portfolio | **Done locally** (device-only and console steps pending) |

**NEXT ACTION:** Phases 0 to 8 are implemented in the working tree and nothing from phases 1 to 8 is committed yet. Work through the manual items at the end of this file, run the device checks, then split the changes into one branch and pull request per phase (Gate 1 first) so CI can confirm each gate.

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
- [x] 1.1 Removed `:download` module, `Screen.Downloads`, bottom-nav tab and unused permissions (commit 37107a7)
- [x] 1.2 Removed 28 template tests and the stray test class in `data/src/main` (commit c4700fd)
- [x] 1.3 Fixed stale tests: `testDebugUnitTest` compiles and passes; assertions updated to the real `networkBoundResource` emission sequence
- [x] 1.4 `ktlintFormat` + manual fixes; `.editorconfig` added (android_studio style, Composable naming, compose wildcard imports); `email_verification`/`forgot_password` packages renamed
- [x] 1.5 Per-module `detekt-baseline.xml`, `maxIssues: 0`, cancellation and cognitive-complexity rules on, Composable-aware thresholds
- [x] 1.6 `:testing` module (`MainDispatcherRule`, fixtures); test deps in library convention plugin; Mockito entries removed
- [x] 1.7 Kover applied to all modules with a merged root report (report only)
- [x] 1.8 `ci.yml` rewritten (PR + main, concurrency, cache, artifacts, secret injection with fork-safe placeholder, wrapper validation). Not yet run on GitHub
- [~] 1.9 Gitleaks job, Dependabot, PR template added. Required status checks still to be enabled after CI is green (see Deferred items)
- [x] 1.10 Parallel + build cache enabled; `force()` block reduced to the stdlib alignment (removing it breaks resolution). Configuration cache is blocked by the current KSP version
- [ ] **Gate 1**

### Phase 2: Data integrity
- [x] 2.1 In-memory Room tests (Robolectric): DAO/data source, category and search mediators
- [x] 2.2 New schema: canonical `movies`, `movie_category(category, movie_id, position)` with FK cascade and indices
- [x] 2.3 Search: `search_result(query, movie_id, position)`, per-query refresh, 24h retention
- [x] 2.4 Single writer: mediators only; list `networkBoundResource` path, non-paged use cases and `discoverMovies` removed; pull-to-refresh uses `LazyPagingItems.refresh()`
- [x] 2.5 Page writes are one transaction in `LocalDataSource` (`saveCategoryPage`, `saveSearchPage`); network fetch happens before any write
- [x] 2.6 `CancellationException` rethrown in `safeApiCall`, mediators, `networkBoundResource`; mediators catch typed errors instead of `Exception`
- [x] 2.7 One `MovieDto.toEntity` mapper; `Constants.TMDB_PAGE_SIZE`; shared `PagedMovieRemoteMediator`
- [x] 2.8 DB reset to v1 with the new schema; `MigrationsUtil.kt` deleted; `DatabaseConfig` in `:database`; schema/migration test harness in `androidTest` (compiles; needs a device to run). Existing dev installs must be uninstalled or have data cleared
- [x] 2.9 `RetryInterceptor` rewritten (cap, jitter, 5xx/429/IOException, idempotent only) with MockWebServer tests; `safeApiCall` tests
- [x] 2.10 Dead code removed (old PagingSources, `ErrorInterceptor`, `PaginatedResult`, `CachePolicy`, rate-limit constant, unused DAO and data source methods)
- [~] **Gate 2**: DAO, mediator and retry tests pass; shared movie visible in two categories; searches do not mix. Coverage gate is configured in Phase 6 (generated code must be excluded first)

### Phase 3: Architecture cleanup
- [x] 3.1 `cachedIn` in the ViewModels; use cases return plain `Flow<PagingData>`
- [x] 3.2 `PaginatedMovieGrid` moved to `:ui`; `search → home` removed
- [x] 3.3 Dispatcher qualifiers and module in `:common`; repositories inject `@IoDispatcher`
- [x] 3.4 Retrofit removed from `:common`; `ErrorMapper` no longer knows HTTP or Firebase
- [x] 3.5 Repository interfaces reduced to what is used; `discoverMovies` and non-paged methods deleted; `MovieRepositoryImpl` documented (stateless, three interfaces)
- [x] 3.6 `AuthDataSource`, `ProfileDataSource`, `CurrentProfileStore` ports with Firebase/DataStore implementations; repositories tested with fakes. Also covers 5.6 (auto-ids, transactional limit and last-profile rule, selection cleared on sign-out)
- [x] 3.7 `ErrorType` carries reasons, not text; `UiText` and string resources in `:ui`; explicit Firebase mapping with tests
- [x] 3.8 Tabs built from `MovieCategory.getAll()`
- [x] 3.9 `streamflix.android.application` plugin for `:app`; unused module dependencies and catalog entries removed; icons version follows the Compose BOM
- [x] 3.10 `Overlay.kt`, `GetMovieDetailsUseCase` and `SearchMoviesPaginatedUseCase` moved to matching packages
- [x] 3.11 `docs/adr/001-module-structure.md`, `docs/adr/002-room-as-single-source-of-truth.md`
- [x] **Gate 3**: `checkModuleGraph` enforces the dependency rule (part of `check` and CI); detekt baselines shrank

### Phase 4: App shell and stability
- [x] 4.1 Bottom bar derived from the back-stack destination (`TopLevelDestination`); Profile tab is the profile management screen. The duplicate `ProfileScreen`/`ProfileViewModel` (never registered) was deleted. Settings is added with Phase 7.6; navigation errors are no longer swallowed (`AppNavigator`)
- [x] 4.2 `AppViewModel` resolves the start destination from session and selected profile (splash held until known) and redirects to Login on sign-out; flow is Login/verification → ProfileSelection → Home; unit tested
- [x] 4.3 `configChanges` removed; trailer position survives rotation via `rememberSaveable`. Needs a device check of every screen under rotation
- [x] 4.4 Trailer chosen by site/type (no `videos[1]`), `TODO()` and `!!` removed, `NavigateToLogin` dropped, stable paging keys; `LoadingIndicator` no longer defaults to hidden
- [x] 4.5 Trailers-only YouTube player tied to the lifecycle; Media3 and the dead controls removed; `docs/adr/003-trailers-only-player.md`
- [x] 4.6 Debug `Log`/`println` removed; Timber is the only logger
- [x] 4.7 Dark `Theme.SplashScreen`, `enableEdgeToEdge` with `SystemBarStyle.dark`, `statusBarColor` removed, insets consumed once in the shell scaffold
- [x] 4.8 Crashlytics and Performance plugins/SDKs added, `CrashlyticsTree` for release, collection disabled in debug. A test crash from a release build must be confirmed manually
- [ ] **Gate 4**: manual smoke test of every tab and the auth flow, and a Crashlytics test crash, are still to be done on a device

### Phase 5: Security and release plumbing
- [x] 5.1 `firestore.rules`, `firebase.json` and emulator tests (16 passing); `firestore-rules` CI job. Rules still have to be deployed to the project
- [x] 5.2 App Check: Play Integrity in release, debug provider in debug. Enforcement is enabled in the console only after a build with App Check is in use
- [x] 5.3 Backup and data-extraction rules exclude everything; `allowBackup=false`
- [x] 5.4 Network security config with pinning for `api.themoviedb.org` (`docs/adr/004-certificate-pinning.md`)
- [x] 5.5 R8 rules rewritten; `assembleRelease` builds with minification. Click-through of the release build still to be done on a device
- [x] 5.6 Profile hardening (auto-ids, transactional limits, current profile cleared on sign-out)
- [x] 5.7 Auth fixes: sign-in no longer applies sign-up password rules, loading guards, independent resend job, non-sensitive fields in `SavedStateHandle`, messages are `UiText`, autofill content types, `FLAG_SECURE` on credential screens
- [x] 5.8 Signing config reads `keystore.properties` or `SIGNING_*` variables. `bundleRelease` with the real keystore is pending
- [x] 5.9 Upgrades: Firebase BoM 34.0.0 (no `-ktx`), coroutines 1.10.2, DataStore 1.1.7, Retrofit 2.11.0, Coil 3. AGP and Kotlin stay as they are (see `docs/SECURITY.md`)
- [x] 5.10 Dependabot covers Gradle, Actions and the Firebase tooling; `docs/SECURITY.md`
- [ ] **Gate 5**: console steps (deploy rules, App Check, enumeration protection), Crashlytics test crash and release smoke test

### Phase 6: Tests to coverage gate  (interleaved with phases 2-5)
- [x] 6.1 Validators and use cases (pass-through use cases are covered by one parameterised-style test)
- [x] 6.2 Auth ViewModels (login, sign-up, email verification, forgot password)
- [x] 6.3 Network (MockWebServer): retry and auth interceptors, safe API call
- [x] 6.4 Repositories and mediators
- [x] 6.5 Remaining ViewModels (home, search, detail, player, profile selection and management, app)
- [x] 6.6 Compose screens under Robolectric (login, detail, error component). More screens are added with the Phase 7 features
- [x] 6.7 Hilt graph smoke test (real graph, Firebase instances replaced). An instrumented end-to-end flow still needs a device
- [x] 6.8 Kover gate enabled: 60% overall, 80% for `:domain` and `:data`; generated code, composables, DTOs and SDK adapters excluded. Coverage badge is added with the README in Phase 8
- [ ] **Gate 6**: confirm the gate on CI

Unit tests run on the debug variant only (release unit tests are disabled in the convention plugins).

### Phase 7: Product features
- [x] 7.1 Home: hero banner (switchable with Remote Config), continue-watching, My List and one paged row per category, skeleton loading and pull-to-refresh
- [x] 7.2 Detail: director, cast, similar titles (cached with the detail), share, My List toggle. Crew beyond the director is not shown
- [x] 7.3 My List (Firestore `users/{uid}/profiles/{pid}/list`, readable offline through the Firestore cache) with its own tab
- [x] 7.4 Continue watching: trailer progress is stored per profile and resumes where it stopped
- [x] 7.5 Search: persisted recent searches (DataStore) and a genre filter applied on the client, because the TMDB search endpoint has no genre parameter
- [x] 7.6 Settings: account, sign out, delete account (re-authenticates, erases Firestore data, then the account), analytics consent, notifications, clear cache, privacy policy link, TMDB attribution. Theme (system, light, dark) and language (Android 13+ per-app language screen) are offered
- [x] 7.7 Remote Config flag with real-time updates, Analytics behind a consent prompt (`docs/ANALYTICS.md`), FCM topic subscription with the Android 13 permission requested on opt-in
- [x] 7.8 Deep link `streamflix://movie/{id}`, honoured only when the app starts signed in. Verified App Links need a domain and are not set up
- [ ] **Gate 7**: needs the Firebase console steps (below) and a device pass

Console steps for this phase: enable Remote Config and add `show_hero_banner`; deploy the updated
`firestore.rules`; enable Cloud Messaging; replace the privacy policy URL in `AppLinks.kt` once the page is live.

### Phase 8: Polish, release, portfolio
- [x] 8.1 Light and dark colour schemes with a Settings choice (system, light, dark); brand name defined once (`brand_name`) so a rename is a one-line change. Dynamic colour is deliberately not used
- [x] 8.2 Accessibility and localisation: all UI strings in resources with a Spanish translation (lint fails on a missing one), headings and content descriptions, per-app language screen, RTL layout test. Not done: a TalkBack walkthrough and a font-scale pass on a device
- [x] 8.3 Adaptive layouts: navigation bar or rail by window size, adaptive poster grids, width-limited forms and detail page
- [x] 8.4 Coil caches configured; `:baselineprofile` module and profile installer added. The profile itself must be generated on a device or emulator (`./gradlew :app:generateBaselineProfile`) and committed
- [x] 8.5 `Release` workflow: runs when a pull request from `release/X.Y.Z` is merged into `main` or `master` and the branch's latest commit message is `release: X.Y.Z` with the same version; builds a signed bundle, with optional Firebase App Distribution and Play internal-track steps that run only when their secrets exist. Without secrets it only attaches an unsigned bundle to the run
- [x] 8.6 Play compliance material: privacy policy page, store checklist and Data Safety answers (`docs/PLAY_STORE.md`), TMDB attribution in Settings. App name, icon and contact email are still to be decided
- [x] 8.7 README with architecture diagram, ADRs 005 and 006, CONTRIBUTING, CHANGELOG. Screenshots are added after a device pass
- [ ] **Gate 8** (Definition of Done, AUDIT section 9): see the remaining manual items at the end of this file

## Deferred items
The `protect-main` ruleset is active (deletion and force-push blocked, PR required). Still pending:

| Item | Trigger | Notes |
|---|---|---|
| Require status checks on `main` (build, lint, tests, coverage) | After Gate 1, once CI is green on a PR | Enabling earlier blocks every merge. Use the exact check names from the workflow |
| Fork PR workflow approval ("Require approval for all outside collaborators") | Before CI uses the TMDB or Firebase secrets (step 1.8) | Settings -> Actions -> General |

## Decision log
| Date | Decision | Why |
|---|---|---|
| 2026-10-07 | Audit first, then implement phase by phase | Every decision is documented with its rationale |
| 2026-10-07 | Repo will be public | Portfolio. Forces strict secret hygiene |
| 2026-10-07 | Delete `:download` for v1 | Uncompilable, and TMDB has no downloadable content source |
| 2026-10-07 | Trailers-only player for v1 (planned, ADR-003) | TMDB provides metadata and YouTube keys, not streams |
| 2026-10-23 | Light and dark themes with a user choice; no dynamic (wallpaper) colour | The audit's Definition of Done asks for both schemes; the brand red stays fixed so the identity is consistent |
| 2026-10-23 | Firebase BoM held at 34.0.0 | Newer BoMs ship Kotlin 2.3 metadata; raise together with the Kotlin upgrade |
| 2026-10-07 | Reset git history rather than only untracking | Two WIP commits, secrets inside, nothing worth preserving |

## Open questions / unverified
- Is the repo already public, and was it cloned? (Rotation done either way.)
- Firebase console: current Firestore rules, App Check, email-enumeration protection (owner to check in Phase 5).
- No device run of the app has been done. Dependency advisories are covered by Dependabot, not a dedicated scanner (Phase 5.10).
- App name and icon: "StreamFlix" resembles Netflix. Decide in Phase 8.6.

## Session log
| Date | Summary | Ended at |
|---|---|---|
| 2026-10-07 | Full audit (verified build run + 5 code audits) → `docs/AUDIT.md`; plan → `docs/PLAN.md`; tracker created. Credentials rotated (0.1). | Next: 0.2 |
| 2026-10-14 | Phase 0 merged (PR #1). Phase 1: 1.1 (download module removed), 1.2 (template tests removed) committed; 1.3 analysed, not yet implemented. | Next: 1.3 |
| 2026-10-23 | Phases 1 to 8 implemented locally with the full gate green (tests, detekt, ktlint, lint, module graph, coverage gate, release build, 27 Firestore rules tests). Nothing committed, per the owner's instruction. | Next: manual items below, then commits and PRs per phase |

## Manual items remaining

Owner-only steps that cannot be done from the code:

- Free disk space regularly: the machine ran out of space twice during the build.
- Uninstall the app from dev devices (the Room schema changed while still at version 1).
- GitHub: add secrets (`TMDB_ACCESS_TOKEN` and `GOOGLE_SERVICES_JSON` are done; for releases add `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`, optionally `FIREBASE_APP_ID`, `FIREBASE_SERVICE_ACCOUNT`, `PLAY_SERVICE_ACCOUNT_JSON`); enable the fork-PR approval setting; make `build`, `firestore-rules` and `secret-scan` required checks once green; enable GitHub Pages from `/docs`.
- Firebase console: deploy `firebase/firestore.rules`; enable App Check enforcement only after a build with App Check is in use; enable Remote Config (`show_hero_banner`), Cloud Messaging and email-enumeration protection; confirm a Crashlytics test crash from a release build.
- Create the release keystore and fill `keystore.properties`.
- Device pass: every tab, sign-in to Home, rotation, light and dark themes, TalkBack, font scale, a tablet or foldable, Spanish.
- Run the instrumented `SchemaTest` and generate the baseline profile (`./gradlew :app:generateBaselineProfile`).
- Decide the app name (the working name resembles a well-known brand), supply the icon, add a contact email to the privacy policy and set the real policy URL in `AppLinks.kt`.
- Add screenshots to the README after the device pass.

