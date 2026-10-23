# Analytics, feature flags and notifications

This page describes what the app collects, how consent works, and how feature flags and
notifications are wired. It is written for users who want to know what is recorded, and for
developers who add events or flags.

## What is collected

Analytics is **off until the user agrees**. The manifest disables Firebase Analytics collection and
the advertising ID, and the app enables collection only while the stored consent is `true`. The
consent prompt appears once, the first time the user reaches the main screens, and the choice can be
changed at any time under *Settings → Privacy*.

Events carry identifiers and counts only. They never include free text, the search query, names or
the account email. Firebase may still attach its standard device and app metadata.

| Event | When it fires | Parameters |
| --- | --- | --- |
| `login` | A password sign-in succeeded | `method` |
| `sign_up` | An account was created | `method` |
| `search_result_opened` | A search result was opened | `query_length` |
| `my_list_add` / `my_list_remove` | The My List button was used on a detail page | `movie_id` |
| `trailer_played` | A trailer started playing | `movie_id` |

Crash reports and performance traces are collected separately by Firebase Crashlytics and
Performance Monitoring so bugs can be fixed. They are described in the
[privacy policy](https://maximum21.github.io/StreamFlix/privacy.html).

### For developers: adding an event

Events are a closed set of Kotlin types, `AnalyticsEvent` in `:domain`. A new event is added there
with its name and parameters, and logged through `AnalyticsTracker`. Keep parameters to identifiers
and counts, and add a row to the table above. The design is explained in
[ADR-006](adr/006-consent-gated-analytics-and-flags.md).

## Remote Config feature flags

| Key | Default | Effect |
| --- | --- | --- |
| `show_hero_banner` | `true` | Shows or hides the featured-title banner on Home |

Defaults are compiled into the app, so it behaves the same without a network. Values are fetched on
start (at most hourly), and real-time updates are applied while the app is running. To use flags in
your own copy, create the key in your Firebase project's Remote Config.

## Notifications

Opting in under *Settings* subscribes the install to the `new_releases` Firebase Cloud Messaging
topic. On Android 13 and later the notification permission is requested at that moment, not at
launch. The messaging token itself is not stored. To send a notification from your own copy, publish
to that topic from the Firebase console.
