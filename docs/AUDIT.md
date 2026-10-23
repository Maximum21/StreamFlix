# StreamFlix: Production-Readiness Audit

- **Date:** 2026-10-07
- **Branch audited:** `work_in_progress` (working tree, about 103 uncommitted entries)
- **Goal:** portfolio piece for Staff-level Android roles, plus a Play Store release
- **Scope of work agreed:** audit only. The owner implements the plan, guided step by step.

**Legend.** **[V]** = verified by running a command or reading the exact code. **[I]** = inferred from reading code, not executed. Severity: **Blocker** (must fix before anything else) > **High** > **Medium** > **Low**.

---

## 1. Executive summary

StreamFlix has a strong skeleton: 15 Gradle modules, convention plugins in `build-logic`, a version catalog, MVI-style contracts, DTO/Entity/Domain separation, and an offline-first design (`RemoteMediator` plus a Room cache). It is **not production-ready**. Four problem groups explain why:

1. **Secrets and repo hygiene.** The TMDB token and `google-services.json` are committed. The files that would protect the repo (`.gitignore`, the CI workflow, `gradlew`) are not committed.
2. **Data integrity.** The movie cache table can't hold one movie in two lists, search paging mixes results between queries, and two pipelines write to the same table.
3. **App shell and stability.** The bottom bar never appears, one tab leads nowhere, and there are crash paths in the player and on the home screen.
4. **Quality gates are red or absent.** All real tests fail to compile, coverage is about 1–5% with no tooling, ktlint fails in 57 tasks, detekt reports 368 findings, and CI has never run on GitHub.

None of this is unusual for an app mid-refactor. The refactor is clearly in progress, with the new `build-logic`, Paging and validation work uncommitted. All of it is fixable in a defined order (see section 10).

### Scorecard

| Area | Grade | One-line reason |
|---|---|---|
| Architecture and modularization | B− | Good module graph and layering. Domain is impure, one feature-to-feature dependency, and an orphan module. |
| Data layer correctness | D | Schema and paging bugs corrupt visible data. |
| Security and secrets | D | Secrets in git history, no Firestore rules, hardcoded credentials in UI defaults. |
| Testing and coverage | F | Zero working real tests, no coverage tooling. |
| CI/CD | D | One 12-line workflow, untracked so it has never run. |
| UI/UX and Compose quality | C− | Works for auth and home. Navigation holes, dark-only, and no previews. |
| Features vs. a Netflix-style app | C− | The catalog and auth work. Everything that makes it a streaming app is missing or a stub. |
| Build and Gradle | C | Convention plugins are good. Performance flags are off, and `force()` hacks and drift remain. |
| Documentation and repo hygiene | F | No README, LICENSE, ADRs or meaningful history. |
| Play Store and legal readiness | D | No TMDB attribution, privacy policy, account deletion or signing. |

---

## 2. How this audit was done

| Method | What it covered |
|---|---|
| Gradle run **[V]**: `./gradlew --continue compileDebugKotlin testDebugUnitTest detekt ktlintCheck lintDebug` | Compile, unit tests, detekt, ktlint, Android lint. Result: **BUILD FAILED in 1m 46s** (824 tasks). |
| Git inspection **[V]** | Tracked files, history, remotes, status. |
| Five read-only code audits | Architecture, tests, security and config, UI and features, plus a second pass over areas the first skipped (profile, download, auth internals, detekt config, Gradle, Room, repo hygiene, resources). |

**Not covered and cannot be:** the Firebase console (Firestore rules in effect, API-key restrictions, App Check, email-enumeration protection), whether the GitHub repo is public, whether the TMDB token is live, and a CVE database scan. I did not run the app on a device. Section 11 lists what to check yourself.

---

## 3. Verified build results

### 3.1 Compilation
- `compileDebugKotlin` for the production code of all included modules: **passes** **[V]**.
- **Three test source sets fail to compile** **[V]**:

| Module | File | Error |
|---|---|---|
| `data` | `MovieRepositoryImplTest.kt:149` | No value passed for parameter `position` |
| `domain` | `GetMoviesByCategoryUseCaseTest.kt:18,26` | Unresolved reference `MovieRepository` (the interface was split into four) |
| `home` | `HomeViewModelTest.kt:60, 75, 94, 112, 126, 143, 158` | Constructor now needs `refreshMoviesUseCase` and a third use case |

### 3.2 Unit tests
- The only tests that ran: **10 tests, 0 failures**, and **all 10 are the Android Studio template `ExampleUnitTest` (`2 + 2`)** **[V]**.
- So the tests that really test something (`HomeViewModelTest`, `GetMoviesByCategoryUseCaseTest`, `MovieRepositoryImplTest`) are exactly the ones that don't compile. Meaningful passing tests: **0**.

### 3.3 Detekt: 368 findings **[V]**
`NewLineAtEndOfFile` 147, `MagicNumber` 81, `WildcardImport` 47, `TooGenericExceptionCaught` 32, `LongMethod` 18, `SwallowedException` 7, `UnusedPrivateProperty` 6, `UnusedParameter` 6, `PackageNaming` 6, `CyclomaticComplexMethod` 4, `MaxLineLength` 3, `LongParameterList` 3.
Detekt did not fail the build, because `config/detekt/detekt.yml` sets a very high `maxIssues` (see F-BUILD-4).

### 3.4 ktlint: **57 tasks failed** **[V]**
Across main, test, androidTest and Kotlin-script source sets in almost every module, and in the root. CI doesn't run `ktlintCheck`, so this is invisible today.

### 3.5 Android lint: 0 errors, 116 warnings **[V]**
99 × `GradleDependency` (outdated libraries), 9 × `AndroidGradlePluginVersion` (AGP 8.9.1; 9.4.1 is available), 7 × `UnusedResources`, 1 × `ModifierParameter`. There are no lint errors only because lint has not been asked about security or correctness beyond defaults.

### 3.6 Environment note **[V]**
The Gradle wrapper pins **8.11.1** but a Gradle **8.10.2** daemon on JDK 17 and an 8.11.1 daemon on **JDK 21** both exist on this machine. The project's `jvmTarget` is 17. Pin the toolchain (`jvmToolchain(17)`, and `JAVA_HOME` in CI) so that "works on my machine" can't diverge.

---

## 4. Blocker findings

### B1. Secrets and key files are tracked in git **[V]**
- `git ls-files` lists `secrets.properties` and `app/google-services.json`. Both entered in commit `f9d6239`. `.gitignore` lists them, but **ignore rules don't apply to already-tracked files**.
- `secrets.properties` contains a live-format `TMDB_ACCESS_TOKEN` (an earlier `TMDB_API_KEY` was in the committed version). `google-services.json` contains a Firebase API key.
- `origin/work_in_progress` exists on `github.com/Maximum21/StreamFlix`, so the commit has left your machine. Whether the repo is public is **unknown**.
- **Why it matters:** anything in git history is recoverable by anyone with clone access. A leaked TMDB token is low-impact, but leaked keys are the first thing a reviewer looks for, and a Firebase key without restrictions is a billing and data risk.
- **Fix (Phase 0):** rotate the TMDB token and re-restrict the Firebase API key to your package name and SHA-1; `git rm --cached` both files; purge from history with `git filter-repo`; commit `secrets.properties.example` and `google-services.json.example`; add secret scanning (Gitleaks) to CI.

### B2. `.gitignore`, CI workflow and `gradlew` are **untracked** **[V]**
- `git status` shows `.gitignore`, `.github/`, `gradlew`, `gradlew.bat`, `config/` (detekt), `database/schemas/`, and every per-module `.gitignore` as `??` (untracked).
- Consequences:
  1. **GitHub has never run your CI.** The workflow file isn't in the repo.
  2. `.gitignore` was never committed, which is how the secrets got in.
  3. Even if CI ran, `./gradlew` and `config/detekt/detekt.yml` aren't there, and `secrets.properties` is the only thing making the build work.
  4. `.idea/` and `.kotlin/` and `build-logic/convention/build/` are not ignored and risk being committed.
- **Fix:** commit `.gitignore` first (before anything else), then `gradlew`, `config/`, `.github/`, `database/schemas/`. Complete the ignore list (`.kotlin/`, `.idea/`, `build-logic/**/build`, `*.hprof`).

### B3. Hardcoded credentials and consent defaults in the UI **[V per audit]**
`auth/.../signup/SignUpContract.kt:6-19` defaults `name`, `email` (a real-looking Gmail address), `password`, `confirmPassword` to concrete values and sets `acceptedTerms = true`. They ship in every build, pre-fill the sign-up form, and pre-ticked consent is invalid under GDPR and Play policy. Treat that password as burned.
**Fix:** default everything to `""`/`false`; prefill only behind `BuildConfig.DEBUG`.

### B4. Movie cache can't represent one movie in two lists **[V per read]**
- `MovieEntity` has `@PrimaryKey val id` only, with `category` and `position` as plain columns, and `insertMovies` uses `REPLACE` (`MovieDao.kt:29`). A movie that is in both Trending and Popular keeps only the last writer's category and position, so it **vanishes from the other list**. The class's own KDoc says one entity can appear in several categories.
- **Fix:** composite key `(id, category)` or a cross-reference table `movie_category(movieId, category, position)` read through a `@Transaction` query, with a real migration (see B6).

### B5. Search paging is wrong **[V per read]**
- `LocalDataSource.kt:66` ignores the `query` argument and reads `WHERE category = 'search'`. `SearchMovieRemoteMediator` never deletes the previous query's rows on REFRESH and writes into the same `category = "search"` bucket (also pulling those movies out of the home categories via B4).
- Result: results from different queries mix, and stale rows survive.
- **Fix:** a `search_result(query, movieId, position)` table, delete by query on REFRESH, read with a join.

### B6. Room migration is broken, and DB version and schema disagree **[V]**
- `database/.../migration/MigrationsUtil.kt:10`: `CREATE TABLE IF NOT EXITS remote_keys` (typo, invalid SQL).
- `Constants.kt:20` sets `DATABASE_VERSION = 1`, but `StreamFlixDatabase.kt` documents v2 and `DatabaseModule.kt` registers `MIGRATION_1_2`. `schemas/1.json` already contains `remote_keys`.
- Today the migration is dead code. After any real version bump it throws on every upgrading install, and only debug builds have a destructive fallback.
- **Fix:** decide the real history. If v1 was never shipped, delete the migration and start at v1 with the composite key. Otherwise fix the SQL. Add `MigrationTestHelper` tests. `DATABASE_VERSION` should live in `:database`, not `:common`.

### B7. Navigation shell is broken **[V per read]**
- `currentScreen` is never updated (`StreamFlixApp.kt:40`, `AppViewModel.kt:83`), so `showsBottomBar()` is always false and the **bottom bar never appears**. This also masks the next bug.
- The Downloads tab calls `navigate(Screen.Downloads)` with no destination registered. The exception is swallowed (`BottomNavigationBar.kt:85`). The Profile tab points at `ProfileManagement`, and `ProfileScreen.kt` (369 lines) is never used. `Screen.Settings` has no destination.
- **Fix:** derive the current route from `navController.currentBackStackEntryAsState()` and `destination.hasRoute<...>()`; register all destinations; don't swallow navigation errors.

### B8. Crash paths **[V per read]**
- `PlayerViewModel.kt:76`: `result.data.videos[1].key` throws `IndexOutOfBoundsException` when a movie has fewer than two videos.
- `HomeScreen.kt:68`: `HomeEffect.NavigateToLogin -> TODO()` throws `NotImplementedError`.
- `PaginatedMovieGridComponent.kt:72`: `key = peek(index)?.id ?: ""` produces duplicate keys while items load, which crashes `LazyGrid`.

### B9. Quality gates are red, and CI is not a real gate **[V]**
See sections 3.1–3.4 and 7. In short: tests don't compile, ktlint fails, detekt is toothless, and CI isn't even committed.

---

## 5. Findings by area

### 5.1 Architecture

| ID | Sev | Finding | Evidence | Fix and pattern |
|---|---|---|---|---|
| A-1 | High | Two competing write paths to the `movies` table: `networkBoundResource` (from `HomeViewModel.init`) deletes and re-inserts page 1 while `CategoryMovieRemoteMediator` owns the paging keys. Positions disagree (`index` vs. `(page-1)*20+index`), and `refreshMovies` deletes rows and keys but never refreshes the Pager. | `HomeViewModel.kt:79-82,101`, `MovieRepositoryImpl.kt:67-102,144-165` | Keep one pipeline: Room as the single source of truth, `RemoteMediator` as the only writer. Pull-to-refresh = `LazyPagingItems.refresh()`. |
| A-2 | High | Mediators clear and insert in separate calls, with no transaction anywhere in `database/` or `data/`. A crash mid-sequence leaves an empty list and no remote key. | `CategoryMovieRemoteMediator.kt:98-139` | `db.withTransaction { }`. |
| A-3 | High | `CancellationException` is swallowed by `catch (e: Exception)`/`Throwable` in `safeApiCall`, both mediators and `networkBoundResource`. Cancelled work is reported as an error and structured concurrency breaks. | `safeApiCall.kt`, mediators `:143-147`, `NetworkBoundResource.kt` | Rethrow `CancellationException` before the generic catch. |
| A-4 | High | Domain layer isn't pure: `GetMoviesByCategoryPaginatedUseCase` takes a `CoroutineScope` and applies `cachedIn`, and `HomeViewModel.kt:73-77` applies it again. The domain module applies the Hilt plugin and the Android library plugin. | `domain/build.gradle.kts` | Accept `PagingData` in the domain as a pragmatic trade-off, but move `cachedIn` to the ViewModel. Consider a pure Kotlin/JVM `:domain`. |
| A-5 | High | Feature-to-feature dependency: `search` depends on `home` to reuse `PaginatedMovieGrid`. | `search/build.gradle.kts:12` | Move shared grid components to `:ui`. Features depend only on core. |
| A-6 | High | `:download` is orphaned and uncompilable (see 5.9 and F-DL). | `settings.gradle.kts` | Delete for v1, or finish properly. |
| A-7 | Med | Dispatcher qualifiers (`@IoDispatcher` etc.) exist in `:app`'s `AppModule.kt:38-102` but nothing injects them. `AuthRepositoryImpl` hardcodes `flowOn(Dispatchers.IO)` six times, so it's harder to test. | `AuthRepositoryImpl.kt:59,102,127,193,215,241` | Move qualifiers and the module to `:common` and inject. |
| A-8 | Med | Mediators build `MovieEntity` by hand (19 lines each) while `MovieEntityMapper` exists, and the paging size constant differs from `Constants.PAGE_SIZE`. | `CategoryMovieRemoteMediator.kt:117-135` | One `MovieDto.toEntity(category, position)` mapper. |
| A-9 | Med | `common` depends on Retrofit (`ErrorMapper` imports `HttpException`) while `safeApiCall` already converts HTTP errors, so the HTTP branch can't be reached. | `common/build.gradle.kts:12`, `ErrorMapper.kt:5` | Map HTTP in `:network` only. |
| A-10 | Med | Inconsistent repository API: `suspend fun ...(): Flow<Result<…>>` (a suspend function returning a cold flow). `ProfileRepository` is not suspend. | `AuthRepository`, `MovieCatalogRepository` | Cold flows are non-suspend. |
| A-11 | Med | One class implements four repository interfaces and is bound four times. `discoverMovies` is a stub (`.filter { true }`, empty save) that nothing consumes. | `RepositoryModule.kt:45-63`, `MovieRepositoryImpl.kt:235-260` | Split by interface or delete the stub. |
| A-12 | Med | `HomeViewModel` keeps both `movieCategories` (legacy non-paged load) and the paged flow, and builds tabs from `movieCategories.keys`. Home data is loaded twice. | `HomeViewModel.kt` | Tabs from a static enum or config. |
| A-13 | Med | Dead code: `CategoryMoviePagingSource`, `SearchMoviesPagingSource`, `ErrorInterceptor` (never installed), `PaginatedResult`, unused `LocalDataSource` methods, `RateLimitInterceptor` (deleted) with `API_RATE_LIMIT_PER_SECOND` still defined. | `data/paging`, `NetworkModule.kt` | Delete. |
| A-14 | Low | Naming drift: snake_case packages (`email_verification`, `forgot_password`), `I`-prefixed interfaces only for data sources, `OverLay.kt`, `GetMovieDetailsUseCase` in `usecase/home`. | detekt `PackageNaming` ×6 **[V]** | Rename once, early. |
| A-15 | Low | Auth/Profile talk to Firebase directly in repositories, with no data-source abstraction like movies have. `NavGraph` takes the concrete `NavigationManagerImpl`, not the interface. | `AuthRepositoryImpl`, `NavGraph.kt` | Introduce `AuthDataSource`/`ProfileDataSource` ports. |

### 5.2 Build and Gradle

| ID | Sev | Finding | Fix |
|---|---|---|---|
| F-BUILD-1 | High | **`network` fails the build without `secrets.properties`** (`FileNotFoundException`), and `buildConfigField` is fed the raw property, so a missing key is `null` and the quotes must already be in the file. `network/build.gradle.kts:10,16` **[V]** | Read only if the file exists, fall back to an env var (for CI), wrap with `"\"…\""`. Use `providers` so the configuration cache works. |
| F-BUILD-2 | High | Performance flags off: `org.gradle.parallel` commented out, no `caching`, no `configuration-cache`, 2 GB heap for 14 modules with KSP, Hilt, Compose and Room **[V]** | Enable parallel, caching and configuration cache, raise the heap to about 4 GB, set `kotlin.daemon.jvmargs`. |
| F-BUILD-3 | High | `allprojects { force(...) }` pins `kotlinx-serialization 1.7.3` and `kotlin-stdlib 2.0.21`, contradicting the catalog (`kotlinxSerialization = 1.8.1`). The catalog is misleading and `force()` hides real conflicts **[V]** | Remove `force()`, use catalog versions, run `dependencyInsight` to find why it was added. |
| F-BUILD-4 | Med | Detekt is lenient: `maxIssues: 150` (default 0), many rules disabled (`CognitiveComplexMethod`, `StringLiteralDuplication`, `LateinitUsage`, `SuspendFunSwallowedCancellation`, `GlobalCoroutineUsage`, most documentation rules), thresholds loosened. Yet the code still produces 368 findings. | Baseline existing issues, `maxIssues: 0`, re-enable the disabled rules (especially cancellation-related ones, which would have caught A-3). |
| F-BUILD-5 | Med | Catalog hygiene: unused `mockito-*` (3), `kotlin-kapt`, `firebase-analytics` (declared, zero events), `gson` (unused or only for JSON columns). Misleading names (`compose-material = 1.6.1` pins `material-icons-extended` independently of the BoM 2025.11; `ktlint` key is the Gradle plugin). Deprecated `retrofit2-kotlinx-serialization-converter` (Retrofit 2.10+ has a built-in one). | Clean up. Take icons from the BoM. |
| F-BUILD-6 | Med | Outdated dependencies (99 lint warnings **[V]**): Firebase BoM 32.7.0 (Jan 2024; `-ktx` artifacts deprecated), coroutines 1.7.3 (lifecycle 2.9 expects newer), Media3 1.2.0, Coil 2.5.0, Retrofit 2.9.0, DataStore 1.0.0, AGP 8.9.1 (9.4.1 available). | Upgrade in one dedicated PR with Renovate or Dependabot configured, then pin. |
| F-BUILD-7 | Med | Modules inconsistent with the convention plugins: `app` and `download` use raw plugin aliases; `app` declares `buildFeatures { compose = true }` twice and has a misleading "explicit API mode" comment; the Compose convention plugin adds `material3` twice (the bundle already has it) and can't apply to `:app`; `model`, `profile`, `ui`, `search`, `detail`, `database`, `network` carry unrelated dependencies (`appcompat`, `material`, `gson`, `media3`, `hilt-navigation-compose`); `domain` applies Hilt without needing it. | Tidy each `build.gradle.kts`, apply convention plugins everywhere, add dependency-analysis if wanted. |
| F-BUILD-8 | Low | Library modules ship empty `consumer-rules.pro`. No `lint {}` blocks or `lint.xml`. No toolchain pin. | Add `jvmToolchain(17)` in convention plugins, one shared lint config. |

### 5.3 Data layer: Room, Paging, Network

| ID | Sev | Finding | Fix |
|---|---|---|---|
| D-1 | Med | No `@Index` anywhere. Queries filter by `category`, order by `position`, and `deleteOldMovies` filters on `cached_at`. | Add indices matching the queries. |
| D-2 | Med | `searchMovies` uses `LIKE '%q%'` (full scan, unbounded, no escaping of `%` and `_`). `getAllMovies()` returns the whole table as a `Flow<List>`. | Page it, or drop it. |
| D-3 | Med | No `@TypeConverters`. `genreIds` is a comma-joined string, and `genres`, `productionCompanies`, `spokenLanguages` and `videos` are JSON strings. Unqueryable, with parsing in mappers (and duplicated genre parsing). | Type converters or normalised tables. |
| D-4 | Med | Cache validity needs three round-trips (`getMovieCountByCategory`, `getNewestCachedAt`, `getCreationTime`). | Single query. |
| D-5 | Med | `RetryInterceptor`: retries only on 429, up to 3, `Thread.sleep` on the dispatcher thread, trusts `Retry-After` unbounded, nothing on IOException or 5xx. It sits ahead of logging, so retries aren't logged. | Cap sleep, add backoff with jitter, skip non-idempotent methods. |
| D-6 | Med | `ErrorInterceptor` calls `printStackTrace()`, and is dead (never installed). | Delete or wire with Timber. |
| D-7 | Low | `ImageUrl` hardcodes `w500` and `original`. | Size by display width (`w342`/`w500`). |
| D-8 | Low | URLs hardcoded in `Constants.kt:10-12` while `secrets.properties` has commented-out copies, so config has two sources. | Put base URLs in `BuildConfig` or one config object. |

### 5.4 Security (OWASP MASVS mapping)

| ID | Sev | Finding | MASVS | Fix |
|---|---|---|---|---|
| S-1 | High | **No Firestore rules, no `firebase.json`, no App Check** in the repo. `AuthRepositoryImpl.kt:72-79` writes `users/{uid}` (email, name, `emailVerified`) from the client. | AUTH-1, STORAGE-2 | Version `firestore.rules` (owner-only; protect `emailVerified`, `uid`, `createdAt`), enable App Check (Play Integrity), restrict the API key. |
| S-2 | High | `proguard-rules.pro` keeps `core.network.model.**`, `core.database.entity.**` and `core.model.**` entirely (defeats obfuscation). One rule is malformed (`-keep, allowobfuscation interface retrofit2.**`). No `-assumenosideeffects` for Log/Timber. No Firestore/Gson rules, yet Gson reads JSON columns and Firestore maps POJOs. | RESILIENCE-3 | Narrow keeps to `@Serializable`/`@Entity`/Firestore models, add log-stripping rules, **smoke-test the release APK** (CI never launches it). |
| S-3 | High | Profile limits are client-side only: max 5 profiles and "can't delete last profile" are read-then-write with no transaction or rule. Profile id is `"profile_${currentTimeMillis()}"`. `MAX_PROFILES` is duplicated three times. `avatarUrl` is any string, and name length is unlimited. | AUTH-1 | Enforce in rules, use auto-ids, single constant, validate. |
| S-4 | Med | `allowBackup="true"`, with empty template `backup_rules.xml` and `data_extraction_rules.xml`. Room and the DataStore (including the current-profile id) get backed up. | STORAGE-1 | `allowBackup="false"` or explicit excludes. |
| S-5 | Med | No `network_security_config`, no cert pinning for `api.themoviedb.org`. Cleartext is blocked by default at targetSdk 36, so the gap is pinning only. | NETWORK-1/2 | Network security config with `cleartextTrafficPermitted=false`, pin the intermediate CA plus a backup pin. |
| S-6 | Med | The TMDB bearer token is embedded in the APK via `buildConfigField` (extractable with apktool). Sent correctly as a header, and `redactHeader("Authorization")` is set **[V]**. | STORAGE-1 | Accept the risk (a free, low-value key) or proxy through a thin backend (a Cloud Function is enough). Document the decision in an ADR. |
| S-7 | Med | `emailVerified` is copied into a client-writable Firestore field. | AUTH-2 | Never use for authorization. Use the ID-token claim, or drop the field. |
| S-8 | Med | Current-profile id survives logout (DataStore key cleared only in `deleteProfile`). After switching accounts, `getCurrentProfile()` returns `Success(null)` for the previous user's id. | AUTH | Clear on sign-out, or key by uid. |
| S-9 | Med | Raw exception text is shown to users (`result.exception.message` in `EmailVerificationViewModel.kt:145`, `ErrorMapper.kt:31`, `ErrorType.toString()`). Firebase auth exceptions map to `Unknown` (see U-10). | CODE-4 | Map error codes to string resources. |
| S-10 | Low | `FOREGROUND_SERVICE` declared with no service or type. `WRITE_EXTERNAL_STORAGE` (≤28) unused. No `POST_NOTIFICATIONS` yet (needed for FCM). | PLATFORM-1 | Remove unused, add what features need. |
| S-11 | Low | Password policy (8+, upper, lower, digit) is acceptable under NIST 800-63B. No autofill hints and no `FLAG_SECURE` on password screens. Login errors are generic, which is good. Check Firebase email-enumeration protection in the console. | AUTH | Add `contentType`, enable the console setting. |
| S-12 | Low | `ReleaseTree` drops all logs, so release errors reach nobody. No Crashlytics. | RESILIENCE | Crashlytics tree for warnings and above. |

### 5.5 Auth and profile logic

| ID | Sev | Finding | Fix |
|---|---|---|---|
| U-1 | High | Password is never validated on login: `passwordValidation` is computed and unused (`LoginViewModel.kt:66`). `LoginState.passwordError` is never set. No `isLoading` guard, so a double-tap sends two logins. | Validate, guard. |
| U-2 | High | `resendVerification()` overwrites the observer's `verificationJob` (`EmailVerificationViewModel.kt:62,128`). The original observer keeps running with no handle, observers can stack, and `NavigateToHome` may fire twice. | Separate jobs. |
| U-3 | High | No runtime auth guard. `AppViewModel` updates `startDestination` on every user change, but `NavHost` ignores it after the first composition. After logout or token expiry the user stays on the current screen. `requiresAuth()` and `isAuthScreen()` are dead code. | Observe auth state in the app shell and `navigate(Login) { popUpTo(0) }`. |
| U-4 | Med | After login the user goes to Home, skipping `ProfileSelection`. The profile feature is effectively bypassed. | Define the flow: Login → ProfileSelection → Home. |
| U-5 | Med | No `SavedStateHandle` in auth VMs. Typed name and email are lost on process death. `configChanges` in the manifest hides this during testing. | Persist non-sensitive fields, remove `configChanges`. |
| U-6 | Med | `collectLatest` is used for one-shot effects (`LoginScreen.kt:64`), which can cancel handling of an earlier effect when a new one arrives. | Use `collect`. |
| U-7 | Med | `SignUpViewModel.kt:96-137` validates against a one-time snapshot, then writes fields piecemeal. Hardcoded "Passwords do not match" and terms toast bypass `strings.xml`. | Validate against fresh state, use resources. |
| U-8 | Low | Search VM filters out queries under 3 characters before `debounce` (`SearchViewModel.kt:~58-70`), so clearing the field keeps the stale results. The IME search action does nothing. `isLoading` is never reset from the paging load state. | Debounce first, then `flatMapLatest { if short, flowOf(PagingData.empty()) }`. |

### 5.6 UI, UX and Compose quality

| ID | Sev | Finding | Fix |
|---|---|---|---|
| X-1 | High | Player lifecycle: ExoPlayer is created in `remember`, not paused on `ON_STOP`, and `keepScreenOn` isn't cleared. `YouTubePlayerView` isn't added as a lifecycle observer or released (WebView leak). The progress loop polls every 500 ms even in YouTube mode and while backgrounded. | Lifecycle observer, release on stop, poll only while playing and resumed. |
| X-2 | High | The Media3 playback path is dead code: `videoType` is hardcoded to `"youtube"` (`PlayerViewModel.kt:75`). Quality, speed and fullscreen controls don't do anything. `startPosition` is commented out, so no resume. No PiP, background audio or MediaSession. | Decide scope: trailers only for v1, or a real Media3 session service. |
| X-3 | High | `Log.e("testingsizes")` in the composition body of `HomeScreen.kt:58` runs on every recomposition. More debug logs in `PlayerScreen.kt:217,334`, `CategoryMovieRemoteMediator.kt:67`, and `println` in `MovieRepositoryImpl.kt:99`, `HomeViewModel.kt:179`. | Remove, use Timber. |
| X-4 | High | Home isn't Netflix-style: a flat two-column grid with tabs. No hero, no horizontal rows. | See Feature gaps. |
| X-5 | Med | Theme is dark-only: `darkTheme` is accepted and ignored (`Theme.kt:51-55`), no light scheme or dynamic colour. `themes.xml` extends `Theme.Material.Light.NoActionBar`, so the first frame flashes white. It doesn't inherit `Theme.SplashScreen`. Colours hardcoded in screens (`Red`, `Color(0xFFFFD700)`). | Tokens plus a real light scheme, a dark `Theme.SplashScreen` parent. |
| X-6 | Med | Edge-to-edge: `statusBarColor` in a `SideEffect` is deprecated and ignored, and `(view.context as Activity)` can crash on a wrapped context. `innerPadding` is applied around the NavHost and then each screen's `Scaffold` adds insets again. | `SystemBarStyle`, apply insets once. |
| X-7 | Med | About 51 hardcoded `Text("…")` literals, plus strings assembled in code. Only `auth` and `app` have `strings.xml`. Blocks localisation. | `stringResource`, `UiText` for ViewModel messages, plurals. |
| X-8 | Med | Accessibility: `MovieCard` is a clickable card without role or click label, the image description duplicates the visible title, 29 `contentDescription = null` (some on informative icons), no headings semantics. | `Card(onClick)`, semantics, TalkBack and large-font testing. |
| X-9 | Med | Stability: zero `@Immutable`/`@Stable`, `List`-holding state classes, `state.movieCategories.keys.toList()` allocates on every recomposition so `CategoryTabs` can't skip. Zero `@Preview` in the repo. | Stability config or `ImmutableList`, `remember(key)`, previews. |
| X-10 | Med | No Coil configuration: default disk cache, no placeholder or error drawables, no crossfade, `rememberAsyncImagePainter` in profile and downloads. Coil 2.5.0 is old. | Singleton `ImageLoader`, size hints. |
| X-11 | Med | Offline: banner on Home only, `AppState.isOnline` never updated, offline error text from `localizedMessage`. | App-level connectivity flow and banner. |
| X-12 | Med | `splash`: `installSplashScreen()` is used, but the theme doesn't inherit `Theme.SplashScreen`. Launcher icon has no monochrome layer. | Fix theme and icon. |
| X-13 | Low | No WindowSizeClass, so the grid is fixed at two columns. `configChanges` in the manifest blocks resizing and locale reload. Navigation transitions are 300 ms fades, with pop transitions mirroring enter. `Detail` pushed onto itself without `launchSingleTop`. | Adaptive layouts, remove `configChanges`, `popEnter`/`popExit`. |
| X-14 | Low | No baseline profile, no `profileinstaller`, no macrobenchmark. | Add. |

### 5.7 Testing and coverage

| ID | Sev | Finding |
|---|---|---|
| T-1 | Blocker | All three real test classes fail to compile **[V]** (section 3.1). CI would be red on its first run. |
| T-2 | Blocker | **No coverage tooling** (no Kover or Jacoco). No coverage in CI. **[V]** |
| T-3 | High | **Zero tests for the riskiest code**: 4 auth ViewModels, validators, `AuthRepositoryImpl`, `AuthInterceptor`, interceptors, DAOs, mediators, all Compose screens, navigation, DI graph. |
| T-4 | High | Test dependencies are wired in only `app`. `network` has only `mockk`. `database`, `detail`, `search` and `ui` have nothing beyond JUnit. No module can write a Compose UI test, and there is no `mockwebserver`, `room-testing`, `paging-testing`, `robolectric`, `core-testing` or `hilt-android-testing` in libraries. |
| T-5 | Med | Template debris: 14 `ExampleInstrumentedTest` and about 14 `ExampleUnitTest` files (`2+2`) inflate counts and give false reassurance. `data/src/main/.../MovieMapperTest.kt` is a test file in the **main** source set (ships in release). |
| T-6 | Med | Existing-test quality: mock-only (no fakes), `relaxed = true` mocks hide missing interactions, `HomeViewModelTest` uses an inline `setMain` with `runTest` on a different scheduler (flaky), `//TODO` placeholders where the error paths should be asserted, `Truth` declared but unused. |
| T-7 | Med | Mockito (3 entries) and mockk coexist, with only mockk used. mockk 1.13.8 and coroutines-test 1.7.3 are stale for Kotlin 2.0.21. No `HiltTestRunner` and no `@HiltAndroidTest`. |

**Estimated coverage today:** about 1–2% effective (5 test classes of about 100 testable classes, and none of them compiles). Meaningful passing tests: **0 [V]**.

### 5.8 CI/CD

| ID | Sev | Finding |
|---|---|---|
| C-1 | Blocker | The workflow isn't committed, so CI has never run on GitHub **[V]** (B2). |
| C-2 | High | The 12-line workflow: triggers on `push` and `pull_request` (every PR builds twice), runs `detekt lintDebug testDebugUnitTest assembleRelease`. It does **not** run `ktlintCheck`, has no wrapper validation, no Gradle cache beyond `setup-gradle`, no test or lint artifacts, no coverage, no concurrency group, no `timeout-minutes`, no permissions block, and `assembleRelease` is **unsigned** and never launched. |
| C-3 | High | It only works with committed secrets (B1, F-BUILD-1). `google-services.json` is required by the Google services plugin, so CI needs it injected from a secret. |
| C-4 | Med | No instrumented tests (emulator or Firebase Test Lab or Gradle managed devices), no dependency-review or CVE scan (OSV-Scanner), no secret scan (Gitleaks), no CodeQL, no Dependabot/Renovate. |
| C-5 | Med | No release pipeline: no signing from secrets, no `bundleRelease` (Play needs AAB), no versioning scheme (`versionCode = 1` hardcoded), no Firebase App Distribution, no Play internal-track upload, no changelog. |
| C-6 | Low | No branch protection or required checks, no PR template, no CODEOWNERS, JDK pinned to 17 in CI but 21 locally. |

### 5.9 Features vs. a Netflix-style TMDB app

| Feature | Status | Notes |
|---|---|---|
| Auth: login, signup, forgot password, email verification | Implemented | Validation exists. See U-1 to U-7. |
| Splash | Implemented | See X-12. |
| Home: category tabs + paged grid | Partial | Flat grid only. |
| Home: hero banner, horizontal carousels | Missing | |
| Search | Partial | Debounce and paging. No history, filters, genre or people search. Bugs B5, U-8. |
| Detail | Partial | Backdrop, info, trailer button. No cast or crew, similar titles, ratings, share or add-to-list. |
| Player: YouTube trailers | Partial | Crash and lifecycle issues (B8, X-1). |
| Player: Media3, PiP, background, subtitles, resume | Stub or missing | |
| Profiles: selection, management | Partial | Reachable only partly. Avatars are a TODO. Bypassed after login. |
| Downloads | **Stub and uncompilable** | See below. |
| My List, favourites, continue watching | Missing | Natural Firestore feature. |
| Settings | Missing | Route declared, no destination. |
| Room cache and RemoteMediator | Implemented, buggy | B4–B6. |
| Offline UX | Partial | X-11. |
| Dark/light theme, dynamic colour | Dark only | |
| Deep links, App Links | Missing | Manifest block commented out. |
| Localization and RTL | Minimal | X-7. |
| Tablet and foldable | Missing | |
| Push notifications (FCM), Remote Config, Analytics events, Crashlytics, Performance | Missing | Requested. Analytics is declared but sends nothing. |
| Baseline profile, macrobenchmark | Missing | |

**`:download` (F-DL, High) [V per read]:** not in `settings.gradle.kts` or `app`; imports `com.asadraza.streamflix.domain.usecase.downloads.*`, which doesn't exist; `Result`, `Download` and `DownloadStatus` are unresolved; build file bypasses convention plugins and targets JVM 11; hardcoded English toasts; `deleteAll`/`deleteSelected` report success regardless of the result; the in-progress click branch is empty; retry just calls resume. **Recommendation:** delete the module and the Downloads tab for v1, and re-introduce downloads later as a proper feature (Media3 `DownloadManager`, a foreground service with `foregroundServiceType`, domain and data layers). Real offline downloads of licensed streams are also not possible with TMDB, which only provides metadata and YouTube trailer keys, so the feature has no legitimate content source anyway.

### 5.10 Repository hygiene and documentation

| ID | Sev | Finding |
|---|---|---|
| R-1 | High | No `README`, `LICENSE`, `CONTRIBUTING`, PR template, ADRs, architecture diagram or docs of any kind **[V]**. |
| R-2 | High | History is two commits ("StreamFlix Work in Progress", "adding paging in progress") with about 103 uncommitted entries (77 modified, +774/−966) including the whole build-logic migration, the auth validation package and the `buildSrc` removal **[V]**. Nothing to bisect, and reviewers can't see how the project evolved. |
| R-3 | Med | `.idea/` and `.kotlin/` are untracked but not ignored. A `MovieMapperTest.kt` stub sits in `src/main`. |
| R-4 | Low | Commits authored with a personal email. Decide the identity you want visible on a portfolio repo. |

### 5.11 Play Store and legal readiness

| ID | Sev | Finding |
|---|---|---|
| P-1 | Blocker for release | **TMDB attribution is missing.** TMDB's terms require its logo and "This product uses the TMDB API but is not endorsed or certified by TMDB." No attribution string or logo exists **[V per search]**. Also review TMDB's terms on caching duration and non-commercial use. |
| P-2 | Blocker for release | No privacy policy (URL on the store listing and in-app). Firebase Auth and Analytics collect personal data and identifiers, so the **Data Safety form** must declare them. |
| P-3 | Blocker for release | **No in-app account deletion** and no web deletion link, which Play requires for apps that create accounts. |
| P-4 | High | EEA/UK **consent for analytics** isn't implemented (and the pre-ticked terms in B3 would be invalid). |
| P-5 | High | No signing configuration (`app/build.gradle.kts:44-45` is a placeholder comment). No AAB pipeline. No Play App Signing or upload key plan. |
| P-6 | Med | YouTube trailers via an embedded player fall under the YouTube API Services Terms and must be disclosed in the privacy policy. |
| P-7 | Med | The name "StreamFlix" and the red-on-dark styling resemble Netflix. Check trademark and Play impersonation policy before publishing, and consider a distinct name and icon. |
| P-8 | Low | Store listing assets (icon, feature graphic, screenshots, content rating questionnaire, target audience) don't exist yet. |

---

## 6. What is already good (keep it)

- Clean module graph: features depend only on core modules (except A-5). Convention plugins and a version catalog are in place.
- Separate DTO, Entity and Domain models, with mappers in `data`.
- Interfaces in `domain`, bound with `@Binds` in `RepositoryModule`. Data sources are behind interfaces.
- Consistent Contract/ViewModel/Screen (State, Event, Effect) structure with `StateFlow` and a `Channel` for effects.
- Offline-first thinking: `networkBoundResource`, a `RemoteMediator` with a cache TTL (`initialize()`), a `CachePolicy` object.
- Typed errors (`ErrorType`, `NetworkError`) with retry helpers. Auth validation messages are already in `strings.xml`. Login errors are generic, which prevents user enumeration.
- Type-safe navigation routes and an injected `NavigationManager`.
- Room schema export is on. `fallbackToDestructiveMigration` is debug-only. The logging interceptor is BODY only in debug and redacts `Authorization`. Timber is planted only for debug.
- `compileSdk`/`targetSdk` 36, `minSdk` 24, core-library desugaring, Java 17, R8 and resource shrinking enabled for release.
- Email verification is enforced at login, with resend.

---

## 7. Test strategy (target)

- **Pyramid:** about 70% JVM unit tests (ViewModels, use cases, validators, mappers, interceptors with MockWebServer), about 20% Robolectric or in-memory integration (Room DAOs, `RemoteMediator` with `paging-testing`, Compose screens), about 10% instrumented (Hilt graph smoke test, login-to-home flow). Snapshot tests (Roborazzi or Paparazzi) for `:ui` components.
- **Conventions:** `given X when Y then Z` names, one test class per production class, hand-written fakes for repositories and data sources, mockk only for interaction checks and never `relaxed` by default, Truth for assertions, Turbine for flows, one shared `MainDispatcherRule`, injected dispatchers.
- **Infrastructure:** a `:testing` module (fixtures, fakes, rules, `HiltTestRunner`), test dependencies in the convention plugin, Kover in the convention plugin.
- **Gates:** start at 60% line coverage on `domain`, `data`, `auth`, `home`, and ramp up to 70–80% on domain and data. Publish the Kover report and JUnit results as CI artifacts, and show a coverage badge in the README.
- **Order:** (1) unblock compilation and delete template tests; (2) test infrastructure; (3) validators and auth use cases; (4) auth ViewModels; (5) network with MockWebServer; (6) DAOs; (7) repositories and mediators; (8) remaining ViewModels; (9) Compose and navigation; (10) Hilt smoke and one end-to-end flow.

---

## 8. Target architecture and the reasoning

```
app ─► feature modules (auth, home, search, detail, player, profile, settings)
          │   depend only on ▼
          ├─► :ui (design system, shared components)
          ├─► :domain (use cases, repository ports, pure Kotlin)
          ├─► :model, :common
app ─► :data (repository adapters) ─► :network, :database, :firebase
```

| Decision | Why |
|---|---|
| **Clean Architecture with a dependency rule** (UI → domain ← data) | Features can be tested without Android or network, and swapping Firebase or Retrofit doesn't touch UI. This is the standard Staff-level interview topic. |
| **MVI per screen** (State, Event, Effect) | One-directional data flow, easy to unit-test (events in, states out), already adopted. Keep it, fix the effect handling. |
| **Room as single source of truth plus `RemoteMediator`** | The UI observes only the DB, so offline, pagination and refresh behave the same. This is why the two-writer problem (A-1) has to go. |
| **Repository ports in `domain`, adapters in `data`** (ports and adapters) | Inversion of control. Add `AuthDataSource` and `ProfileDataSource` ports (A-15). |
| **Convention plugins** | One place for Android, Hilt, Compose, test and Kover config, so modules stay a few lines. |
| **Hilt with injected dispatchers** | Deterministic tests, no hidden `Dispatchers.IO`. |
| **Firestore for user data** (profiles, My List, continue watching, settings) with owner-only rules and a local cache | Cross-device sync for a small amount of per-user data. Rules plus App Check are the server-side authority. |
| **Firebase suite** | Crashlytics (crashes), Analytics (events, with consent), Performance (traces), Remote Config (flags), FCM (push), App Distribution (beta builds in CI). |
| **ADRs** for each decision | Staff-level signal: trade-offs written down, not just code. |

---

## 9. Definition of Done for "production-ready portfolio"

- [ ] No secrets in the repo or history. Tokens rotated. Secret scan in CI.
- [ ] `./gradlew build` is green locally and in GitHub Actions: compile, unit tests, ktlint, detekt (0 new issues), lint (0 errors), release bundle.
- [ ] Coverage ≥ 60% overall and ≥ 80% on `domain` and `data`, enforced by Kover, with a badge.
- [ ] No known crash path. `TODO()` and debug logging removed. Crashlytics live.
- [ ] Data layer correct: composite or cross-ref key, per-query search table, one writer, transactions, migration tests.
- [ ] Navigation complete: bottom bar, all tabs, runtime auth guard, deep links.
- [ ] Firestore rules versioned and tested with the emulator, App Check on.
- [ ] Release signing via CI secrets, R8 rules verified on a launched release build, AAB uploaded to Play internal testing.
- [ ] TMDB attribution, privacy policy, Data Safety form, in-app account deletion, analytics consent.
- [ ] Accessibility pass (TalkBack, font scale, contrast), localisation-ready strings, light and dark themes, tablet layouts.
- [ ] README with screenshots, architecture diagram, module map, setup instructions (including secrets), CI and coverage badges, plus ADRs and a CHANGELOG.
- [ ] Git history that tells a story: small, conventional commits on feature branches merged via PRs.

---

## 10. Phased roadmap (what, why, and in what order)

The rule behind the order: **stop the damage → make the foundation trustworthy → build on it.** Features built on a corrupt cache or a red CI would have to be redone.

| Phase | Work | Why now | Concepts you'll learn |
|---|---|---|---|
| **0. Secret hygiene and repo baseline** | Rotate the TMDB token and Firebase key restrictions. Commit `.gitignore` first. `git rm --cached` the secrets, `filter-repo` the history, add `*.example` files, commit `gradlew`, `config/`, `.github/`, `schemas/`. Add the Gradle secrets fallback (F-BUILD-1). | Leaked keys are permanent, and an untracked `.gitignore` caused this. Nothing else is safe until the repo is. | Secrets as build inputs, tracked vs. ignored files, history rewriting |
| **1. Make CI honest** | Fix or delete the 3 stale tests, delete about 28 template tests, create `:testing`, add Kover, run `ktlintFormat` and baseline detekt, `maxIssues: 0`, rewrite the workflow (PR-only plus main push, concurrency, caching, artifacts, `ktlintCheck`, wrapper validation, Gitleaks, Dependabot). | You need a reliable safety net before refactoring. | Test pyramid, fakes vs. mocks, quality gates |
| **2. Data integrity** | Composite key or join table, per-query search table, single writer, `withTransaction`, rethrow `CancellationException`, fix the migration and add migration tests, indices, converters. | B4–B6 and A-1 to A-3 corrupt visible data, and the schema change gets harder with every feature on top. | Single source of truth, `RemoteMediator`, structured concurrency |
| **3. Architecture cleanup** | Purify `domain`, move shared grids to `:ui`, inject dispatchers, delete dead code, decide `:download` (delete for v1), unify the error model with `UiText`, finish the convention plugins, remove `force()`, enable build performance flags. | It's cheap now and expensive after features land. | Dependency rule, ports and adapters, convention plugins |
| **4. App shell and stability** | Route-derived bottom bar, all destinations, runtime auth guard, Login → ProfileSelection → Home, fix crashes (B8), player lifecycle, theme and splash, edge-to-edge, Crashlytics tree. | Makes the app actually usable and observable. | Type-safe navigation, lifecycle-aware resources |
| **5. Security and release plumbing** | Remove hardcoded credentials, Firestore rules plus emulator tests, App Check, backup rules, network security config and pinning, R8 rules, signing config, Firebase BoM and dependency upgrades. | The MASVS items a staff-level reviewer checks. | Defense in depth, least privilege, R8 |
| **6. Tests to the coverage gate** | Validators, auth VMs, network (MockWebServer), DAOs, mediators, repositories, remaining VMs, Compose screens, Hilt smoke test. | Coverage is credible only on the stable code from phases 2–5. | Turbine, Robolectric, TDD habits |
| **7. Product features** | Home hero and rows, detail cast and similar, My List and continue watching (Firestore), Settings, Remote Config flags, FCM, Analytics events with consent, search history. | Now the feature work sits on solid ground. | Offline-first sync, feature flags, event design |
| **8. Polish and Play** | Design system, light theme, i18n, accessibility, adaptive layouts, baseline profile, App Distribution and Play internal-track pipeline, TMDB attribution, privacy policy, account deletion, store listing, README, ADRs, diagrams. | Turns working code into a portfolio piece. | Material 3, Macrobenchmark, release engineering |

Estimated effort: phases 0–3 about 1.5 weeks, phases 4–6 about 2 weeks, phases 7–8 about 2–3 weeks of focused part-time work.

---

## 11. Items only you can verify (outside the repo)

1. Is `github.com/Maximum21/StreamFlix` public? Has anyone cloned it? (Affects urgency of B1.)
2. Firebase console: current Firestore rules (test mode?), API-key restrictions, App Check status, Auth email-enumeration protection, which sign-in providers are enabled.
3. TMDB account: regenerate the access token and API key.
4. Run a CVE scan (`osv-scanner` or the OWASP dependency-check plugin).
5. Launch a minified release build on a device after the R8 changes.

---

## 12. Appendix: commands used and how to reproduce

```bash
./gradlew --continue compileDebugKotlin testDebugUnitTest detekt ktlintCheck lintDebug
git ls-files | grep -Ei "secrets|google-services|keystore|\.jks"
git status --short | grep '^??'
```

Detekt and ktlint reports: `*/build/reports/`. Lint report: `app/build/reports/lint-results-debug.html`.
