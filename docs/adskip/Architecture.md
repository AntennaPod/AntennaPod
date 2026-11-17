# AdSkip – Architecture

## 1. Purpose & scope
AdSkip adds automatic and manual ad skipping to a personal AntennaPod fork. It works on downloaded episodes using audio fingerprinting plus simple signal processing. There is no LLM and no cloud service. The app **skips during playback** and never rewrites audio files.

In scope: fingerprinting, repeat discovery, segment labeling, skip playback with ping/undo, review UI, hold-to-mark, waveform editor, a per-show + opt-in global library, auto-backup, and a standalone PC bulk tool.
Out of scope: streaming-only analysis, transcript/LLM detection, cut-file export, the Tailscale/RSS server, iOS, Play Store distribution, a shared/community ad-fingerprint server (possible later; needs its own moderation and privacy design).

## 2. Key decisions
| Decision | Choice | Rejected alternatives |
|---|---|---|
| Base | Fork AntennaPod `develop` (GPL-3.0, Media3 1.10, compileSdk 36) | From scratch (re-implements RSS/download/player); MinusPod (LLM-centric) |
| Removal method | Skip lists applied at playback | Re-encoding cut files (slow, destructive, needs FFmpeg) |
| Precise skip trigger | Media3 `ExoPlayer.createMessage(...).setPosition(ms)` | Upstream's 1 s position poll (too coarse; poll is only a fallback) |
| Decoding | Platform `MediaExtractor` + `MediaCodec` → PCM | FFmpegKit (retired by its author Apr 2025; forks exist but it's a heavy dependency) |
| Fingerprint engine | Own landmark (peak-pair hash) engine, Kotlin/JVM | Olaf/Panako via JNI (license + build complexity; revisit only if too slow) |
| Storage | Separate AdSkip DB (Room) + per-episode fingerprint blob files | Tables in upstream DB (merge conflicts); fingerprints in SQLite rows (slow bulk lookups) |
| minSdk | Raise the fork's minSdk to 31 (Android 12) | Keep 23 (more testing surface, no benefit for personal use) |
| Distribution | Public GPL-3.0 repo; signed APKs on GitHub Releases (installable with Obtainium); F-Droid later via the `free` flavor | Google Play (policy bans apps that "block or interfere with another app displaying ads"; enforcement unpredictable) |
| Name | Renamed fork with its own icon; "Based on AntennaPod" credit | Keeping the AntennaPod name (confusing; AntennaPod's community has pushed back on look-alike forks) |

## 3. Modules
```
adskip/
  api/        Java-friendly interfaces + static entry points called by upstream hooks (AdSkipHooks)
  core/       Pure Kotlin/JVM: spectrogram, peaks, hashing, matching, repeat clustering,
              classification, edge snapping, skip-list merge. No Android imports.
  storage/    Room DB, fingerprint blob store, episode identity, backup/restore (zip)
  analysis/   Android: MediaCodec decode → PCM, WorkManager jobs, triggers, back-catalog runner
  playback/   Skip controller for ExoPlayer (PlayerMessage), ping player, undo, cast fallback
  ui/         Screens, seek-bar segment overlay, hold-to-mark button, waveform editor, settings,
              strings
  cli/        JVM app (PC bulk tool) built on :adskip:core; decodes via an ffmpeg subprocess
```
Dependency direction: `ui → playback, analysis → storage → core`; `api` depends on all of them and is the only module upstream code imports.

## 4. Upstream hook points (keep each to a few lines)
| # | File | Hook |
|---|---|---|
| H1 | `settings.gradle`, `app/build.gradle` | include `:adskip:*`; `app` depends on `:adskip:api` |
| H2 | `common.gradle` | `minSdk 31` |
| H3 | `app/build.gradle` | own `applicationId` (e.g. `io.github.<you>.adskip`) + app name so it installs alongside stock AntennaPod |
| H4 | `net/download/service/.../episode/MediaDownloadedHandler.java` (`run()`, after the media is saved) | `AdSkipHooks.onEpisodeDownloaded(context, media)` |
| H5 | `playback/service/.../internal/ExoPlayerUtils.java` (where the `ExoPlayer` is built) | `AdSkipHooks.onExoPlayerCreated(exoPlayer)`. `Media3PlaybackService.player` is a `ForwardingPlayer` around a possibly-cast player and doesn't expose `createMessage`, so the controller needs the real ExoPlayer here |
| H6 | `Media3PlaybackService.java`: `onMediaItemTransition` and wherever `currentPlayable` changes | `AdSkipHooks.onMediaChanged(media)` |
| H7 | `Media3PlaybackService.java` position observer (1 s interval) | `AdSkipHooks.onPositionTick(position)`. Fallback skipping only when casting or when no ExoPlayer is available |
| H8 | `app/.../playback/audio/ChapterSeekBar.java` | draw segment blocks from `AdSkipHooks.segmentsFor(media)` |
| H9 | `app/.../playback/audio/AudioPlayerFragment.java` | add the hold-to-mark button (view from `:adskip:ui`) |
| H10 | `app/.../episodeslist/EpisodeItemViewHolder.java`, `app/.../screen/episode/ItemFragment.java` | "N ads dropped" badge; link to the Lost content screen |
| H11 | `app/.../feed/preferences/FeedSettingsFragment.java` | entry to per-show AdSkip settings |
| H12 | main preferences screen (`:ui:preferences`) | entry to global AdSkip settings |

| H13 | `net/common/.../UserAgentInterceptor.java` | User agent `AdSkip/<version>`, so upstream's podcast directories and servers don't attribute this fork's traffic to AntennaPod |
| H14 | `net/discovery/build.gradle` | No change to code. Builds pass the fork's own `podcastindexApiKey`/`podcastindexApiSecret` Gradle properties (free key from podcastindex.org) instead of falling back to AntennaPod's bundled key |
| H15 | app icon, launcher name, About screen, `README.md` | New name and icon; About keeps upstream credits and the license and adds "Based on AntennaPod (GPL-3.0)" |

Upstream's existing per-feed "skip intro/ending seconds" (`SkipUtils`) stays untouched and still works.

## 5. Data model (AdSkip DB, Room)
**Episode identity.** Upstream DB row IDs change on reinstall and don't exist on the PC, so all AdSkip records use
`episodeKey = sha1(feedDownloadUrl + "\n" + (itemGuid ?: enclosureUrl))` and
`showKey = sha1(feedDownloadUrl)`. A lookup table maps them to upstream `FeedMedia.id` at runtime.

Tables:
- **AnalyzedFile**: `episodeKey` (PK part), `fileFingerprint` (PK part: size + sha1 of the first and last 64 KB), `showKey`, `durationMs`, `fpVersion`, `analyzedAt`, `status` (PENDING/DONE/FAILED/STALE), `source` (PHONE/PC_IMPORT). A re-downloaded file with different content gets a new row; the old row's occurrences are marked STALE. This handles dynamically inserted ads.
- **Segment**: a canonical repeated clip. `id`, `showKey` (null if global), `isGlobal`, `label` (AD, INTRO, OUTRO, DISCLAIMER, BUMPER, UNKNOWN_REPEAT, MANUAL_AD), `action` (SKIP/KEEP), `origin` (AUTO/MANUAL/IMPORT), `refEpisodeKey`, `refStartMs`, `refEndMs`, `createdAt`, `userConfirmed` (bool).
- **Occurrence**: `id`, `segmentId`, `episodeKey`, `fileFingerprint`, `startMs`, `endMs`, `matchScore`, `state` (ACTIVE, RESTORED_HERE, STALE).
- **ShowSettings**: `showKey`, `enabled`, `trigger` (AFTER_DOWNLOAD/CHARGING_IDLE/ON_DEMAND), `backCatalogN`, `minRepeatEpisodes` (default 2), `minSegMs` (10 000), `maxSegMs` (180 000), per-label actions (INTRO/OUTRO/DISCLAIMER/BUMPER → SKIP or KEEP; default KEEP), `reactionOffsetMs` (1500).
- **GlobalSettings**: defaults for the above, backup folder URI, backup interval, ping sound, undo window (5 s).

**Files** (app-private storage):
- `fp/<showKey>/<episodeKey>_<fileFingerprint>.fp`: binary, versioned header, then sorted `(hash:int32, tMs:int32)` pairs.
- `env/<episodeKey>_<fileFingerprint>.env`: loudness envelope (RMS in dB, 10 Hz, int8). Used for edge snapping and the waveform editor without decoding again.

**Fingerprints and envelopes outlive the audio files.** When AntennaPod deletes a played episode, its fingerprints stay, so the show library keeps growing.

Skip list for a file = ACTIVE occurrences whose segment `action == SKIP`, merged where they overlap or have gaps < 1 s, computed by `:adskip:core`.

## 6. Analysis pipeline (`:adskip:analysis`)
1. A trigger enqueues a unique WorkManager job per episode. Constraints depend on the show's trigger setting: none / `requiresCharging + requiresDeviceIdle` / manual.
2. Decode with `MediaExtractor` + `MediaCodec` to PCM. Downmix to mono and resample to 8 kHz while streaming, so the whole file is never held in memory.
3. `core.fingerprint(pcm)` → hashes + envelope; write the files.
4. Build an in-memory index for the show (hash → list of (episodeKey, tMs)) from that show's `.fp` files, plus global segments' reference hashes. Cache it while the job runs.
5. `core.findRepeats` → `core.cluster` → `core.classify` (see Detection.md) → upsert Segments and Occurrences.
6. Post an event so open UI refreshes. Notify only if the user opted in.

**Back catalog:** when AdSkip is enabled for a show, enqueue the last N episodes that are downloaded. Episodes that aren't downloaded are listed with a "download for analysis" action, which by default deletes the file after fingerprinting.

## 7. Backup & restore (`:adskip:storage`)
- Periodic WorkManager job (modeled on upstream's `AutomaticDatabaseExportWorker`) writes `adskip-backup-<date>.zip` to a user-chosen SAF folder: `manifest.json` (format version, fpVersion, app version), an export of the AdSkip DB, and the `fp/` and `env/` trees. Keep the last 3.
- Restore merges by `episodeKey` + `fileFingerprint`. User-confirmed labels win over automatic ones.
- **The same zip format is the import format for the PC tool.**

## 8. PC bulk tool (`:adskip:cli`)
- A runnable JVM jar. No server, no Tailscale, no RSS hosting.
- `adskip-cli analyze --feed <rssUrl> --last N --out bundle.zip` downloads the enclosures itself (plain HTTP to public feeds), decodes them with `ffmpeg -i f -ac 1 -ar 8000 -f s16le -`, and runs the same core pipeline.
- `adskip-cli analyze --dir <folder> --feed <rssUrl>` matches local files to items by enclosure filename.
- Import on the phone: Settings → AdSkip → Import bundle.
- **Caveat:** with dynamically inserted ads, the PC's copy of an episode may contain different ads than the phone's copy. The import seeds **Segments and the fingerprint library**. Occurrences for the phone's own files are still computed on the phone, which is fast once the library exists.

## 9. Dependencies
- Existing: Media3 1.10.0, WorkManager 2.10.3, Kotlin 2.3.20 (plugin already in the version catalog).
- New: AndroidX Room (latest stable) for `:adskip:storage`. An FFT: a small radix-2 real FFT in `:adskip:core` (no dependency) or JTransforms. Benchmark both in M1.
- PC tool: JDK 21, `ffmpeg` on PATH.

## 10. Risks
- Upstream refactors of the playback service can break H5–H7. Keep hooks tiny and re-check them after each upstream merge.
- `PlayerMessage` is `@UnstableApi` in Media3. Pin the Media3 version together with upstream and cover it with an instrumented test.
- Casting: no `PlayerMessage` on a remote player, so fall back to the 1 s poll (skips may land up to ~1 s late).

## References
- AntennaPod source (cloned 2026-11-15, `develop`): module list, `SkipUtils`, `Media3PlaybackService`, `MediaDownloadedHandler`, `AutomaticDatabaseExportWorker`, `common.gradle`, `gradle/libs.versions.toml`
- Media3 PlayerMessage: https://developer.android.com/reference/androidx/media3/exoplayer/PlayerMessage
- FFmpegKit retirement: https://awesome.ecosyste.ms/projects/github.com%2Farthenica%2Fffmpeg-kit
- Google Play Device and Network Abuse policy: https://support.google.com/googleplay/android-developer/answer/16559646
