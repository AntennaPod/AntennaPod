# :storage:database

AntennaPod's main database, containing subscriptions and playback state (but not user settings).
Uses raw SQLite via `PodDBAdapter` (not Room); writes go through async `DBWriter`; reads use cursor-to-object mappers in the `mapper/` package.

Adding a `FeedPreferences` field requires touching five places: the model class in `:model`, then in `PodDBAdapter` the `KEY_` constant, `CREATE_TABLE_FEEDS`, the `KEYS_FEED` select list and `setFeedPreferences()`, then `mapper/FeedPreferencesCursor`, a new `oldVersion <` block in `DBUpgrader` and a bump of `PodDBAdapter.VERSION`.
Omitting the `KEYS_FEED` select list makes `FeedPreferencesCursor` throw at runtime, because it resolves every column with `getColumnIndexOrThrow`.

`FeedDatabaseWriter.updateFeed(context, feed, removeUnlistedItems)` deletes stored items that are absent from the parsed feed, except downloaded ones of remote feeds.
Callers must only pass `true` when the parsed item list is exhaustive, so never for a single page of a paged feed.
