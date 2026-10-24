# ADR-005: Per-profile library data and account deletion

Status: accepted

## Context

My List and watch history belong to a profile, not to a device, so they live in Firestore under
`users/{uid}/profiles/{profileId}/list` and `.../history`. Play requires apps that create accounts
to let users delete the account and its data from inside the app, and the identity provider only
accepts deletion shortly after authentication.

## Decision

- Library data is stored per profile with the title and poster path copied into each document, so
  a list renders without a catalogue request. Firestore's local cache makes it readable offline.
- Deleting an account re-authenticates with the password, then erases data in this order:
  sub-collections of each profile in batches, then all profiles together with the user document,
  and only then the account. The account goes last because once it is gone the security rules no
  longer authorise the data deletes. If the last step fails, the user can sign in and retry.
- The rules allow the last profile to be removed only in the same write that removes the user
  document, and require every profile create or delete to move the counter by exactly one.
- Everything is client-side. A Cloud Function would be more robust against a client that dies
  halfway, but it needs the Blaze plan; the retry path covers the failure case for v1.

## Consequences

- The deletion code and the rules must change together; both are covered by tests (the rules by
  emulator tests in CI).
- A user who signed in with a stale session must type their password again to delete the account.
