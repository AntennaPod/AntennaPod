# :storage

Data storage for podcast data (subscriptions, playback state) and settings (but not settings UI).
`storage:database` uses raw SQLite (not Room); `storage:preferences` wraps SharedPreferences.
