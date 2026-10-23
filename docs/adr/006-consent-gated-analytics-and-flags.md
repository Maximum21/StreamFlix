# ADR-006: Consent-gated analytics and Remote Config flags

Status: accepted

## Context

Usage analytics are useful but optional, and the app has users in regions where collection needs
opt-in consent. Remote Config lets some behaviour change without a release but must never make
the app depend on a network round trip.

## Decision

- Analytics collection is disabled in the manifest (including the advertising ID). The app
  enables it only while the stored consent is `true`, and asks once, on first reaching the main
  screens, never over the sign-in flow.
- Events are a closed set of Kotlin types in `:domain` (`AnalyticsEvent`) and carry identifiers
  and counts only; the search text is never recorded. `docs/ANALYTICS.md` lists them.
- Crash reporting stays on in release builds. It is disclosed in the privacy policy and the
  Data Safety form, and contains no user content.
- Remote Config values have compiled-in defaults applied before any fetch, are fetched at start
  with an hourly minimum interval, and real-time updates are applied while the app runs. A flag
  read never blocks on the network.
- Notifications use an FCM topic subscription, so no token is stored server-side, and the
  Android 13 permission is requested when the user turns the setting on.

## Consequences

- A new event or flag is a code change in one place with its documentation next to it.
- Analytics counts only reflect users who opted in, which is intended.
