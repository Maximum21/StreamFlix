# Analytics

Analytics is off until the user agrees. The manifest disables Firebase Analytics collection and
the advertising ID, and the app enables collection only while the stored consent is `true`. The
consent prompt appears once, the first time the user reaches the main screens, and the choice can be
changed under Settings → Privacy.

Events carry identifiers and counts only. They never include free text, the search query, names
or the account email. Firebase may still attach its standard device and app metadata.

| Event | When it fires | Parameters |
| --- | --- | --- |
| `login` | A password sign-in succeeded | `method` |
| `sign_up` | An account was created | `method` |
| `search_result_opened` | A search result was opened | `query_length` |
| `my_list_add` / `my_list_remove` | The My List button was used on a detail page | `movie_id` |
| `trailer_played` | A trailer started playing | `movie_id` |

New events are added to `AnalyticsEvent` in `:domain`, which keeps this list and the code in step.

## Remote Config

| Key | Default | Effect |
| --- | --- | --- |
| `show_hero_banner` | `true` | Shows or hides the featured-title banner on Home |

Defaults are compiled in, so the app behaves the same without a network. Values are fetched on
start (at most hourly) and real-time updates are applied while the app is running.

## Notifications

Opting in under Settings subscribes the install to the `new_releases` FCM topic. On Android 13+
the notification permission is requested at that moment, not at launch. The token itself is not
stored.
