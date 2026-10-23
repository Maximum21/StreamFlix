# Contributing

## Setup

1. Use JDK 17 and a recent Android Studio.
2. Copy `secrets.properties.example` to `secrets.properties` and add a TMDB read access token.
3. Place your own `google-services.json` in `app/` (both files are git-ignored).
4. `./gradlew assembleDebug`

## Before opening a pull request

```bash
./gradlew --continue compileDebugKotlin testDebugUnitTest checkModuleGraph detekt ktlintCheck lintDebug \
  koverVerify :domain:koverVerify :data:koverVerify
(cd firebase && npm ci && npm test)   # when changing firestore.rules
```

CI runs the same checks. `ktlintFormat` fixes most style findings.

## Conventions

- **Modules:** dependencies point towards the core and features never depend on each other.
  `checkModuleGraph` enforces it; see `docs/adr/001-module-structure.md`.
- **Architecture:** MVI per screen (state, events, one-off effects). ViewModels depend on use cases,
  never on Firebase, Room or Retrofit.
- **Errors:** carry reasons and codes (`ErrorType`), not English text; the UI maps them to
  strings. Always rethrow `CancellationException`.
- **Strings:** all user-visible text lives in `strings.xml` with a Spanish translation; lint fails on a
  missing one.
- **Tests:** new logic comes with tests. Domain and data keep 80% line coverage.
- **Commits:** Conventional Commits in the imperative mood (`feat: add watch history`), one
  logical change per commit. Branches are named `type/short-description`.
- **Secrets:** never commit tokens, `google-services.json`, keystores or `secrets.properties`.
  Gitleaks runs on every pull request.

## Architecture decisions

Significant choices are recorded in `docs/adr/`. Add a new numbered record when you change one.
