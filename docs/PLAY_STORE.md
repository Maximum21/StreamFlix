# Play Store release checklist

## Before the first upload

- [ ] Decide the final app name. The working name resembles a well-known streaming brand and risks
      a trademark rejection. Rename by changing `brand_name` in `ui/src/main/res/values/strings.xml`;
      the launcher label, sign-in screen, Home title and consent prompt all follow it.
- [ ] Provide the launcher icon (adaptive icon with a monochrome layer for themed icons).
- [ ] Publish `docs/privacy.html` (Settings → Pages → deploy from `/docs`) and put the live URL in
      `AppLinks.PRIVACY_POLICY` and in the Play Console.
- [ ] Add a contact email to the privacy policy and the store listing.
- [ ] Create the release keystore, enrol in Play App Signing and add the CI signing secrets.
- [ ] Upload the first bundle manually and add the signing and Play secrets, then cut releases by merging a pull request from a `release/X.Y.Z` branch (latest commit message `release: X.Y.Z`) into `main`.

## Store listing

- Short description: "Discover films, watch trailers and keep a list of what to watch next."
- Mention in the description: "This product uses the TMDB API but is not endorsed or certified by TMDB."
- Screenshots: phone (Home, Detail, Search, My List), plus 7-inch and 10-inch tablet shots now that
  the layouts adapt.

## Data safety form

| Question | Answer |
| --- | --- |
| Does the app collect or share user data? | Collects; does not share with third parties for their own use |
| Personal info | Email address, name (account) |
| App activity | App interactions (analytics, optional), in-app content such as My List and watch history |
| App info and performance | Crash logs and diagnostics |
| Device or other IDs | Firebase installation and messaging identifiers |
| Is all data encrypted in transit? | Yes |
| Can users request deletion? | Yes, in-app (Settings → Delete account) and through the policy page |
| Optional data | Analytics and notification identifiers are optional and off by default |

## Policy points to confirm

- Account deletion is available in the app and described on the privacy policy page.
- The TMDB attribution is shown under Settings → About and in the store listing.
- Trailers play only through the YouTube embedded player, and its terms are linked in the policy.
- Target audience: 13+ (not designed for children).
- Content rating questionnaire: the app shows film metadata and trailers and has no user-generated
  content or purchases.
