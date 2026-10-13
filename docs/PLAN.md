# StreamFlix: Remediation Plan

Companion to `docs/AUDIT.md` (finding IDs like B1, A-3, S-2 refer to it).

**Working agreement**
- Every step lists what changes, why, and which pattern it follows, so each decision can be explained later.
- One branch per piece of work (`type/short-description`, for example `chore/repo-baseline`), small commits in Conventional Commits style (`fix(data): …`), merged to `main` through a PR. Each PR must pass CI once CI exists (Phase 1).
- Decisions already made: the repo will be **public**; the **`:download` module and tab are deleted** for v1; Firebase services in scope are Auth + Firestore, Crashlytics + Analytics + Performance, Remote Config + App Distribution, and Cloud Messaging.
- Each phase ends with a **gate**: a checklist that must be true before the next phase starts.

**Why public matters:** anything committed is permanently visible and indexed. That makes Phase 0 mandatory, and it changes one decision: `google-services.json` and all keys stay out of the repo (CI injects them from GitHub secrets), and we ship `*.example` templates instead.

---

## Phase 0: Secret hygiene and repo baseline  (blocks everything; ~0.5 day)

**Goal:** a repo that is safe to make public. **Fixes:** B1, B2, R-3, F-BUILD-1 (part).

### 0.1 Rotate credentials FIRST (history rewrite doesn't un-leak a key)
1. TMDB: Settings → API → regenerate both the v3 API key and the v4 read access token. Put the new token in your local `secrets.properties` only.
2. Firebase/Google Cloud: Credentials → open the Android API key → restrict to *Android apps* with package `com.asadraza.streamflix` and your debug and release SHA-1s. If in doubt, delete the key and re-download `google-services.json`.
3. The sign-up default password and email in `SignUpContract.kt` (B3): change that password anywhere you use it.

*Why:* the old values stay in `origin/work_in_progress` and in anyone's clone. Only rotation makes them worthless.

### 0.2 Commit the files that protect the repo (on a new branch)
```bash
git switch -c chore/repo-baseline
git add .gitignore            # FIRST, alone
git commit -m "chore: add gitignore"
```
Extend `.gitignore` before that commit with: `.kotlin/`, `.idea/`, `build-logic/**/build/`, `*.hprof`, `local.properties`, `secrets.properties`, `keystore.properties`, `*.jks`, `google-services.json`.

### 0.3 Untrack the secrets (working copies stay on disk)
```bash
git rm --cached secrets.properties app/google-services.json
git commit -m "chore: stop tracking local secrets"
```
Create `secrets.properties.example` (placeholders only) and `app/google-services.json.example`.

### 0.4 Remove the secrets from history (public-repo requirement)
Your history is two commits of work-in-progress, so the cleanest option is a **fresh history**: that also fixes R-2 (no meaningful history to show).
1. Back up: `cp -R . ../StreamFlix-backup` (outside the repo).
2. Finish phase 0.2–0.3 and the remaining baseline files (below) so the tree is clean of secrets.
3. Create an orphan branch with one clean initial commit, replace `main`, delete `work_in_progress` on the remote:
```bash
git checkout --orphan clean-main   # NOT `git switch --orphan`: that deletes tracked files from the working tree
git add -A && git status        # read the list: no secrets, no .idea, no build/
git commit -m "chore: initial import (history reset to remove leaked credentials)"
git branch -M main
git push --force origin main
git push origin --delete work_in_progress
```
Alternative if you want to keep history: `git filter-repo --path secrets.properties --path app/google-services.json --invert-paths`. Either way, GitHub keeps cached views of old commits for a while, so rotation (0.1) is the real fix. After the force-push, contact GitHub support to purge cached commits only if the repo was public before.

### 0.5 Commit the rest of the baseline
`gradlew`, `gradlew.bat`, `gradle/wrapper/*`, `config/detekt/detekt.yml`, `.github/workflows/ci.yml`, `database/schemas/`, per-module `.gitignore`, `build-logic/`, `docs/`. Add `LICENSE` (MIT or Apache-2.0) and a stub `README.md`.

### 0.6 Make the build work without secrets (F-BUILD-1)
In `network/build.gradle.kts`: read `secrets.properties` only if it exists, fall back to `System.getenv("TMDB_ACCESS_TOKEN")`, and wrap the value as `"\"$value\""`. Pin the JDK with `jvmToolchain(17)` in the convention plugins.

### 0.7 Remove hardcoded credentials (B3)
`SignUpContract.kt`: all defaults `""`, `acceptedTerms = false`. Optional debug prefill behind `BuildConfig.DEBUG`.

**Gate 0:**
- [ ] New TMDB key in use. Firebase key restricted.
- [ ] `git ls-files | grep -Ei "secrets.properties$|google-services.json$|\.jks"` returns nothing.
- [ ] `git log -p --all | grep -c <old token prefix>` returns 0 (use the first 6 characters of the old token).
- [ ] Fresh clone + `secrets.properties` from the example builds (`./gradlew assembleDebug`).
- [ ] `.github/workflows/ci.yml` is on GitHub (it may fail; that's Phase 1).

---

## Phase 1: Make CI honest  (~2 days)

**Goal:** a green, meaningful pipeline before any refactoring. **Fixes:** B9, T-1, T-2, T-4, T-5, C-1 to C-4, F-BUILD-4 (part).

1. **Delete `:download`** (module folder, its `Screen.Downloads`, bottom-nav item, `FOREGROUND_SERVICE` and `WRITE_EXTERNAL_STORAGE` permissions, commented service in the manifest). This is the first commit, and it removes an uncompilable module.
2. **Delete template tests** (`ExampleUnitTest`, `ExampleInstrumentedTest`) and move/delete `data/src/main/.../MovieMapperTest.kt`.
3. **Fix or rewrite the 3 stale tests** so `testDebugUnitTest` compiles and passes (use the new constructors and `MovieCatalogRepository`).
4. **ktlint:** run `./gradlew ktlintFormat`, then fix the rest by hand. Commit formatting changes separately from logic.
5. **Detekt:** generate a baseline (`detektBaseline`) so existing findings don't block, set `maxIssues: 0`, re-enable `SuspendFunSwallowedCancellation`, `GlobalCoroutineUsage`, `StringLiteralDuplication`, `CognitiveComplexMethod`.
6. **Create `:testing` module** (test fixtures, fakes, `MainDispatcherRule`, later `HiltTestRunner`). Put test deps (`junit`, `mockk`, `turbine`, `truth`, `coroutines-test`, `mockwebserver`, `room-testing`, `paging-testing`, `robolectric`, `core-testing`) in the convention plugin so every module gets them. Remove the three Mockito entries.
7. **Kover** via the convention plugin. Report only at first (no gate yet).
8. **Rewrite `ci.yml`:** trigger on `pull_request` and push to `main`; `concurrency` group with cancel-in-progress; `timeout-minutes`; `permissions: contents: read`; `gradle/actions/wrapper-validation`; Gradle cache; steps `ktlintCheck detekt lintDebug testDebugUnitTest koverXmlReport assembleDebug`; upload test, lint and Kover reports as artifacts; inject `google-services.json` and `TMDB_ACCESS_TOKEN` from GitHub secrets.
9. **Add** Gitleaks job, Dependabot (`.github/dependabot.yml` for gradle and github-actions), PR template, branch protection requiring CI.
10. **Gradle performance:** `org.gradle.parallel=true`, `org.gradle.caching=true`, `org.gradle.configuration-cache=true`, heap 4 GB, `kotlin.daemon.jvmargs`; remove the `force()` block and fix whatever conflict it was hiding (F-BUILD-2, F-BUILD-3).

**Why this order:** every later phase needs a trustworthy red/green signal. The baselines keep today's debt from blocking you while stopping new debt.

**Gate 1:** CI green on a PR; test report and Kover report downloadable; Gitleaks clean; branch protection on.

---

## Phase 2: Data integrity  (~3 days)

**Goal:** the cache stores and pages data correctly. **Fixes:** B4, B5, B6, A-1, A-2, A-3, A-8, D-1 to D-4.

Write the tests **first** (in-memory Room + `paging-testing`), watch them fail on today's code, then fix. This is the best learning opportunity in the plan.

1. **Schema:** introduce `movie_category(movieId, category, position)` joined to `movies(id)`, or composite key `(id, category)`. I recommend the join table: one canonical movie row, many list memberships (normalised cache). Add indices on `(category, position)` and `cached_at`.
2. **Search:** `search_result(query, movieId, position)`; delete by query on REFRESH; the DAO query takes `query`.
3. **Single writer:** delete the `networkBoundResource` path for lists (`loadAllCategories`, `refreshMovies` write path). Pull-to-refresh calls `LazyPagingItems.refresh()`.
4. **Transactions:** wrap clear, key insert, and movie insert in `db.withTransaction { }`.
5. **Cancellation:** rethrow `CancellationException` in `safeApiCall`, both mediators, `networkBoundResource`.
6. **One mapper:** `MovieDto.toEntity(category, position)`; one page-size constant.
7. **Migration:** since the app was never released, reset to DB version 1 with the new schema, delete `MigrationsUtil.kt` (typo goes with it), move `DATABASE_VERSION` into `:database`. From now on, every schema change gets a migration plus a `MigrationTestHelper` test.
8. **Retry interceptor** (D-5): cap `Retry-After`, add jittered backoff, retry IOException and 5xx for idempotent methods only. Test with MockWebServer.
9. Delete dead code: old `PagingSource`s, `ErrorInterceptor` or wire it properly, `PaginatedResult`, `RateLimit` constant, unused DAO methods.

**Gate 2:** DAO, mediator and migration tests pass; same movie visible in two categories; two different searches don't mix; Kover for `:database` and `:data` ≥ 60%.

---

## Phase 3: Architecture cleanup  (~3 days)

**Fixes:** A-4 to A-7, A-9 to A-15, F-BUILD-5 to F-BUILD-8.

1. Move `cachedIn` from the use case into the ViewModel; use cases return `Flow<PagingData<Movie>>`.
2. Move `PaginatedMovieGrid`/`MovieGrid` into `:ui` and drop `search → home`.
3. Move dispatcher qualifiers and module to `:common`; inject `@IoDispatcher` everywhere (replace the six `flowOn(Dispatchers.IO)`).
4. Remove Retrofit from `:common`; map HTTP only in `:network`.
5. Repository API: cold flows are not `suspend`. Split `MovieRepositoryImpl` per interface or document why not; delete `discoverMovies`.
6. `AuthDataSource` and `ProfileDataSource` ports (Firebase behind interfaces) so repositories are unit-testable with fakes.
7. Error model: `ErrorType` carries codes, not English text; a `UiText` abstraction maps to string resources; map Firebase auth and Firestore exceptions explicitly.
8. Tabs from a static category enum, not from `movieCategories.keys`.
9. Apply convention plugins to `app` where possible; clean per-module dependencies (`model` should have none of `appcompat`/`material`); remove unused catalog entries; fix `compose-material` pin to come from the BoM.
10. Rename snake_case packages, `OverLay.kt`, move `GetMovieDetailsUseCase`.
11. Write **ADR-001** (module structure and dependency rule), **ADR-002** (Room as single source of truth).

**Gate 3:** no feature-to-feature dependency (add a Gradle check or `dependency-guard`); `domain` has no Android imports except Paging (documented in the ADR); detekt baseline shrinks.

---

## Phase 4: App shell and stability  (~3 days)

**Fixes:** B7, B8, U-3, U-4, X-1, X-3, X-5, X-6, X-12, S-12.

1. Bottom bar from `currentBackStackEntryAsState()` + `hasRoute<…>()`; register Profile (use `ProfileScreen`), Settings (stub screen, filled in Phase 7). Don't swallow navigation exceptions.
2. Auth guard in the app shell: observe auth state, `navigate(Login) { popUpTo(0) }` on sign-out; flow Login → ProfileSelection → Home.
3. Remove `configChanges` from the manifest; fix what that exposes (state restoration).
4. Crash fixes: trailer selection by `type == "Trailer" && site == "YouTube"`, empty state when none; implement or delete `NavigateToLogin`; stable keys via `movies.itemKey { it.id }`.
5. Player lifecycle: `DefaultLifecycleObserver`, release/pause on stop, poll only while playing; clear `keepScreenOn`. For v1 scope the player to **YouTube trailers only**, and delete the unused Media3 code path (write ADR-003 explaining why: TMDB has no streams).
6. Remove all debug logs; Timber everywhere.
7. Theme: dark `Theme.SplashScreen` parent, `SystemBarStyle`, insets applied once; remove `statusBarColor`.
8. **Firebase Crashlytics + Performance** and a `CrashlyticsTree` for Timber release builds.

**Gate 4:** manual smoke test of every tab and the full auth flow; no `TODO()`; Crashlytics test crash received.

---

## Phase 5: Security and release plumbing  (~2 days)

**Fixes:** S-1 to S-11, P-5, C-5 (part), F-BUILD-6.

1. `firestore.rules` (owner-only; immutable `uid`/`createdAt`; profile name length, avatar allowlist, max 5 profiles enforced server-side), `firebase.json`, emulator tests (`@firebase/rules-unit-testing`), run in CI.
2. Enable **App Check** (Play Integrity; debug provider for debug builds).
3. Backup: `allowBackup="false"`; fill `data_extraction_rules.xml`.
4. `network_security_config.xml` (no cleartext; pin TMDB intermediate CA + backup pin) with an ADR on the pinning-rotation risk.
5. R8: replace blanket keeps with targeted ones (`@Serializable`, `@Entity`, Firestore models), add `-assumenosideeffects` for `Log` and Timber verbose/debug; **build a release APK and click through the app**.
6. Profiles: auto-ids, single `MAX_PROFILES`, transaction for create and delete, clear the current-profile key on sign-out.
7. Auth fixes: login password validation and loading guard (U-1), separate jobs in email verification (U-2), `SavedStateHandle` for non-sensitive fields (U-5), `collect` for effects (U-6), localised errors (S-9), autofill hints and `FLAG_SECURE` on password fields.
8. Signing: `keystore.properties` locally, GitHub secrets in CI; `bundleRelease`; Play App Signing.
9. Dependency upgrade PR: Firebase BoM (move off `-ktx`), coroutines, Media3 (or remove), Coil 3, Retrofit with built-in kotlinx converter, DataStore, AGP; one library group per commit.
10. Run OSV-Scanner and fix anything high or critical.

**Gate 5:** rules tests green; release APK launches and works; OSV clean; `docs/SECURITY.md` written.

---

## Phase 6: Tests up to the coverage gate  (~1.5 weeks, interleaved with 2–5)

Order: validators and use cases → auth VMs → network (MockWebServer: 200/401/429/500/timeout/malformed) → repositories and mediators (fakes) → remaining VMs → Compose screens (Robolectric) → navigation → Hilt smoke test + one login-to-home instrumented flow. Conventions are in AUDIT §7.

**Gate 6:** Kover gate enabled at 60% overall, 80% on `domain` and `data`; Kover badge in README.

---

## Phase 7: Product features  (~2 weeks)

Each feature follows the same recipe: domain model and use case (tests first) → repository port + Firestore adapter → ViewModel (contract) → screen → tests.

1. **Home:** hero banner + horizontal `LazyRow` sections (`LazyColumn` of rows), skeleton loading.
2. **Detail:** cast and crew, similar titles, ratings, share.
3. **My List** (Firestore `users/{uid}/profiles/{pid}/list`) with offline cache.
4. **Continue watching / watched trailers** (Firestore), with resume position for trailers.
5. **Search:** history (DataStore), recent searches, genre filter.
6. **Settings:** theme, language, clear cache, analytics consent, sign out, **delete account** (P-3).
7. **Remote Config:** feature flags (e.g. new home layout) with safe defaults; **Analytics** events with a documented taxonomy and consent gate (P-4); **FCM** with `POST_NOTIFICATIONS` runtime permission and a topic subscription.
8. Deep links (`streamflix://movie/{id}` and verified App Links if you own a domain).

**Gate 7:** each feature has tests; Remote Config flag toggles without a rebuild.

---

## Phase 8: Polish, release and portfolio  (~1 week)

1. Design system: tokens, light + dark + dynamic colour, shared components (hero, row, section header, empty and error states, skeleton), `@Preview`s, stability config.
2. Accessibility pass (TalkBack, font scale 200%, contrast, touch targets), strings to `strings.xml` in every module, RTL check, at least one extra locale.
3. Adaptive layouts with WindowSizeClass (grid columns, navigation rail on tablets).
4. Coil `ImageLoader` (cache sizes, placeholders, `w342`/`w500` by width). Baseline profile + Macrobenchmark module; record startup numbers in the README.
5. CD workflow: tag → `bundleRelease` signed → Firebase App Distribution (testers) → Play internal track. `versionCode` from the CI run number; auto changelog.
6. Play compliance: TMDB logo and attribution text in Settings/About; privacy policy page (GitHub Pages) linked in-app and on the listing; Data Safety form; YouTube disclosure; content rating; **rename the app and icon** to avoid Netflix confusion (P-7).
7. Portfolio docs: README (hero screenshots/GIF, features, architecture diagram, module map, tech stack with reasons, setup incl. secrets, test and coverage commands, CI/coverage badges), `docs/adr/*`, `CONTRIBUTING.md`, `CHANGELOG.md`, Mermaid diagrams.

**Gate 8 = Definition of Done** (AUDIT §9).

---

## Tracking

Create a GitHub Project (or `docs/PROGRESS.md`) with one card per numbered task above; link the audit finding ID in each PR description so reviewers can trace every fix back to the audit.

## Starting point (Phase 0)

Start with 0.1 (rotate the keys). Tell me when it's done and I'll walk you through 0.2 onward, step by step, explaining each command before you run it.
