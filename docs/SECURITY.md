# Security

This page explains how the app approaches security, what it deliberately does not protect, and what
to do if you run your own copy. It is written for people reviewing the project, contributors, and
anyone who forks it.

## Reporting a vulnerability

Please report suspected vulnerabilities privately with GitHub's **Report a vulnerability** option on
the repository's Security tab, not as a public issue. The scope is this repository: the app, the
Firestore rules, and the build and release workflows. Problems in TMDB, Firebase or Google Play
themselves should go to those providers. An acknowledgement should arrive within a few days.

## How the app is protected

| Area | Approach |
| --- | --- |
| Transport | HTTPS only. Cleartext traffic is disabled in the manifest and in the network security config. |
| Certificate pinning | `api.themoviedb.org` is pinned to the Amazon Root CA 1 and the Amazon RSA 2048 M04 intermediate, with an expiry date. Image CDN hosts are not pinned. See [ADR-004](adr/004-certificate-pinning.md). |
| Authentication | Firebase Authentication with required email verification. Passwords are never persisted, including in saved state. Credential screens block screenshots and declare autofill hints. |
| Authorization | Firestore rules are owner-only and validate the shape, type and size of every document. They are covered by tests that run against the Firebase emulator in CI. |
| Abuse protection | Firebase App Check: Play Integrity in release builds and the debug provider in debug builds. |
| Local data | Backups and device transfer are disabled. Release builds are minified with R8, and verbose and debug logging is stripped. |
| Secrets | The TMDB token and `google-services.json` come from local git-ignored files or CI secrets. CI scans the full git history for secrets on every run. |
| Release pipeline | Building and publishing are separate jobs, so distribution credentials never reach the Gradle build. Publishing runs in a protected `release` environment, and the third-party actions that receive credentials are pinned to commits. |

## Known trade-offs

These are deliberate and worth knowing before you rely on the app:

- **The TMDB token is inside the app.** It is compiled into `BuildConfig`, so anyone can extract it
  from the APK. Treat it as a public, read-only key: rotate it if it is abused. Real protection would
  mean proxying TMDB through a backend that checks App Check.
- **The profile limit is an app rule, not a security boundary.** At most five profiles per account
  keeps the app consistent, and the Firestore rules help maintain it, but a user editing their own data
  by hand can only affect their own account.
- **Deleting an account happens on the device.** It re-authenticates, erases the user's data and
  then the account, so a failure at the last step can leave an account with no data. It can be retried
  ([ADR-005](adr/005-account-deletion-and-library-data.md)).
- **The deep link uses a custom scheme** (`streamflix://movie/{id}`), which any app can claim. It only
  carries a public movie id and is ignored unless the user is already signed in.

## Running your own copy

- Use a Firebase project of your own and deploy `firebase/firestore.rules` to it. The rules are
  part of the security model; without them Firestore is open or closed depending on its defaults.
- Keep `google-services.json`, `secrets.properties` and any keystore out of git. They are already in
  `.gitignore`.
- Turn on App Check enforcement only after a build that includes App Check is in use, and register
  the debug token for local builds.
- Create your own keystore. Never reuse a debug key for a published build.

## Dependencies

GitHub's dependency alerts cover known advisories. A Dependabot configuration for Gradle, GitHub
Actions and the Firebase test tooling is included in `.github/dependabot.yml`, commented out; remove
the leading `# ` to enable it. Its ignore rules reflect one deliberate pin:

- The Firebase BoM is held at 34.0.0. Later releases ship Kotlin 2.3 metadata, which this project's
  Kotlin 2.0.21 compiler cannot read, so the BoM should move together with a Kotlin upgrade.
