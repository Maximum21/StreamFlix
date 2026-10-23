# Security

## Reporting a vulnerability

Please report suspected vulnerabilities privately through GitHub's "Report a vulnerability" option
on the repository's Security tab instead of opening a public issue. Expect an acknowledgement
within a few days.

## Design summary

| Area | Approach |
| --- | --- |
| Transport | HTTPS only. Cleartext traffic is disabled in the manifest and the network security config. |
| Certificate pinning | `api.themoviedb.org` is pinned to the Amazon Root CA 1 and the Amazon RSA 2048 M04 intermediate, with an expiry. Image CDN hosts are not pinned. See ADR-004. |
| Secrets | The TMDB token and `google-services.json` come from CI secrets or local, git-ignored files. Gitleaks runs on every PR. |
| Backups | `allowBackup` is off and the backup and device-transfer rules exclude all data. |
| Authentication | Firebase Auth with required email verification. Passwords are never persisted, including in saved state. Credential screens set `FLAG_SECURE` and declare autofill content types. |
| Authorization | Firestore rules are owner-only, validate profile fields and cap the profile count. They are covered by emulator tests that run in CI. |
| Abuse protection | Firebase App Check: Play Integrity in release builds, the debug provider in debug builds. |
| Release build | R8 shrinking and obfuscation with targeted keep rules. Verbose and debug logging is stripped. Crashes go to Crashlytics. |

## Dependency hygiene

Dependabot opens weekly update PRs for Gradle, GitHub Actions and the Firebase test tooling, and
GitHub's dependency alerts cover known advisories. Versions that are pinned below the latest
release, and why:

- Firebase BoM is held at 34.0.0. Later releases ship Kotlin 2.3 metadata, which the project's
  Kotlin 2.0.21 compiler cannot read. Raise it together with the Kotlin upgrade.
