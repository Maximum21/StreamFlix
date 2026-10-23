# Changelog

All notable changes are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/),
and versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Home with a hero banner, continue watching, My List and paged rows per category.
- My List, watch history with trailer resume, recent searches and a genre filter.
- Detail pages with cast, director, similar titles and sharing.
- Settings: account deletion, theme, analytics consent, notifications, language, cache clearing.
- Analytics behind a consent prompt, Remote Config flags, FCM topic notifications and a
  `streamflix://movie/{id}` deep link.
- Spanish translation and adaptive navigation (bottom bar, rail) and layouts.
- Firestore security rules with emulator tests, App Check, certificate pinning for the TMDB API.
- CI (static analysis, tests, coverage gate, secret scan). A release workflow runs when a `release/X.Y.Z` pull request with a `release: X.Y.Z` commit is merged into `main`; it publishes only when the signing and Play secrets exist.

### Changed
- Offline-first data layer: Room is the single source of truth and RemoteMediators are the only writers.
- Authentication, profiles and storage sit behind ports with fake-backed tests.
- Player plays official YouTube trailers only.

### Removed
- The unfinished downloads module and the unused ExoPlayer code path.
