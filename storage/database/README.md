# :storage:database

AntennaPod's main database, containing subscriptions and playback state (but not user settings).
Uses raw SQLite via `PodDBAdapter` (not Room); writes go through async `DBWriter`; reads use cursor-to-object mappers in the `mapper/` package.
