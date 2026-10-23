# ADR-003: Trailers-only playback through the YouTube player

Status: accepted

## Context

The app's catalogue comes from TMDB. TMDB provides metadata and, per title, a list of videos
(trailers, teasers, clips) that are hosted on YouTube or Vimeo. It does not provide streams of
the films themselves, so there is nothing for a general-purpose player such as Media3/ExoPlayer
to play.

The first version of the player carried both an ExoPlayer path (with quality and speed
selectors) and a YouTube path. The ExoPlayer path could never receive a URL, and the YouTube
path picked `videos[1]`, which crashed for titles with fewer than two videos and chose the wrong
video for many others.

## Decision

- Version 1 plays official YouTube trailers only, through the YouTube player library that embeds
  the IFrame player. The trailer is the first video with `site == "YouTube"` and
  `type == "Trailer"` (`MovieDetail.officialTrailer`).
- The Media3 dependency, the custom controls, the quality and speed selectors and the stream-URL
  use case are removed. The YouTube view provides its own controls.
- A title without a trailer shows an explicit empty state; the Detail screen only offers
  "Play Trailer" when a trailer exists.
- The player view is tied to the lifecycle: it is registered as a lifecycle observer (playback
  pauses when the screen stops) and released when the composable leaves composition. The
  playback position survives configuration changes.

## Consequences

- Playback depends on YouTube availability and on the embedded player's terms; a release
  checklist item covers the YouTube API Services terms and the TMDB attribution requirement.
- If a licensed streaming source is added later, a new ADR should introduce a player abstraction
  at that point, instead of keeping an unused one now.
