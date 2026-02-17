# Dev Banner Conventions

The debug-only banner in `main.xml` surfaces a short status line while `BuildConfig.DEV_DIAGNOSTICS` is true. Fields are space-delimited and prefixed:

- M: module name (current fragment simple name, e.g., `HomeFragment`, `QueueFragment`, `SubscriptionFragment`).
- C: context code. `Q` when the app is operating in queue-centric mode, `P` when podcast-centric mode is active.
- S: stream id. `None` when nothing is playing; otherwise the feed media id of the currently playing item.
- T: autoplay toggle state. `ON` when the current feed’s autoplay is enabled, `OFF` when disabled, `UNK` if not resolved.
- QT: continuous-playback toggle. `ON` when the user setting to follow the queue (continuous playback) is enabled; `OFF` when disabled.

Release builds set `DEV_DIAGNOSTICS=false`, so the banner and its data are excluded from user-facing builds.
