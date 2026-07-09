# :playback:cast

This module provides Chromecast support for the Google Play version of the app.
Compiled only in the `play` build flavor (the `free` flavor contains no-op stubs).

## Active path (Media3)

`CastPlayerWrapper.wrap(localPlayer, context)` builds a Media3 `CastPlayer` that automatically
routes playback to the Cast device when a session is active and falls back to the local player
otherwise. It uses `ApMediaItemConverter` (inner class) to translate between Media3 `MediaItem`
and Cast `MediaQueueItem`. The media database ID is round-tripped through Cast metadata under
the key `"media_id"`, which `Media3PlaybackService.ensureCurrentMediaLoaded()` reads back to
look up the `FeedMedia` from the local database.

## Legacy path (CastPsmp)

`CastPsmp` implements `PlaybackServiceMediaPlayer` for the old `PlaybackService`. It is still
compiled but is effectively dead code — `PlaybackService` throws an exception if started.
