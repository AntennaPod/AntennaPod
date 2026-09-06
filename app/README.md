# :app

The main application module that integrates all features and hosts app-specific UI screens not large enough for their own module.
`PodcastApp` initializes the app; `ClientConfigurator` registers service implementations (download, sync) at startup.

The miniplayer (the collapsed player bar at the bottom of the screen) is implemented in `ExternalPlayerFragment`.
It is hosted in `MainActivity` as a bottom sheet. `MainActivity` controls its visibility via `setPlayerVisible()` based on playback state.

The expanded player is `AudioPlayerFragment` for both audio and video. `CoverFragment` creates its video view
and connects its Media3 controller only for video episodes, attaching the view only while resumed and the bottom sheet
is expanded; detaching the view leaves audio playing.
Full-screen video uses `Media3VideoPlayerActivity` in the same task, so Back returns to the expanded player.
The cover uses the normal screen timeout and keeps audio playing when the screen turns off.
Full-screen and PiP video pause on screen-off, but must not pause audio episodes.
Before attaching its video view, the cover closes any PiP video window through `VideoPlayerViewAttachedEvent`.

## Wear OS Communication (play flavor only)

`WearListenerService` is a `WearableListenerService` that handles `DataLayer` messages from connected watches.
It responds to watch-initiated requests.
The service is kept alive by the Android framework while at least one watch is connected.
