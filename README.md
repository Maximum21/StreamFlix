# StreamFlix

A Netflix-style movie discovery app for Android, built with Jetpack Compose, a multi-module Clean Architecture, Room, Paging 3, Hilt and Firebase. Movie data comes from the TMDB public API.

> **Status:** work in progress. A production-readiness audit and remediation plan are tracked in [`docs/`](docs/).

## Setup

1. **TMDB token.** Create a free account at [themoviedb.org](https://www.themoviedb.org/settings/api), copy your *API Read Access Token*, then:
   ```bash
   cp secrets.properties.example secrets.properties
   # edit secrets.properties and set TMDB_ACCESS_TOKEN
   ```
2. **Firebase.** Create a Firebase project, register the Android app `com.asadraza.streamflix`, download `google-services.json` and place it in `app/`. Neither file is committed (see `.gitignore`).
3. **Build.** Use JDK 17 and run `./gradlew assembleDebug`.

## Documentation

- [`docs/AUDIT.md`](docs/AUDIT.md): findings and rationale
- [`docs/PLAN.md`](docs/PLAN.md): remediation plan by phase
- [`docs/PROGRESS.md`](docs/PROGRESS.md): current status

## Attribution

This product uses the TMDB API but is not endorsed or certified by TMDB.

## License

[MIT](LICENSE)
