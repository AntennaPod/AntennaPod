# AdSkip – Playback & UI

## 1. Purpose & scope
How skip lists are applied during playback, and every screen and control the user touches. Modules: `:adskip:playback`, `:adskip:ui`, plus hooks H5–H12 (Architecture.md §4).

## 2. Skip controller (`:adskip:playback`)
- **Attach:** on `onMediaChanged`, load the skip list for the file and cancel old messages. For each `SkipRange` send `exoPlayer.createMessage(target).setPosition(startMs).setLooper(mainLooper).setDeleteAfterDelivery(false).send()`.
- **On delivery:** if the player is playing and the range isn't suppressed → play the ping, `seekTo(endMs)`, start the undo window, increment the episode's dropped count.
- **User seeks into a range on purpose** (position lands strictly inside a range after a user seek): don't skip that range during this play session. The user's intent wins.
- **Undo window** (default 5 s): show a snackbar in the app, plus a notification/media-session custom action "Go back" when the player screen isn't visible. Undo → `seekTo(startMs)` and suppress that range for this session. A second action, "Not an ad", sets the Occurrence to RESTORED_HERE.
- **Ping:** a short (~150 ms) tone played through a separate `SoundPool` with `USAGE_MEDIA`, at a user-set volume. Built-in choices: soft tick / chime / off.
- **Cast / no ExoPlayer:** fall back to `onPositionTick` (1 s). Skip when `startMs ≤ pos < endMs − 1500`.
- **Live updates:** if a skip list changes while playing (e.g. a new manual mark), re-attach without interrupting playback.
- Coexists with upstream skip intro/ending. If they overlap, whichever seeks further wins.

## 3. Screens & controls (`:adskip:ui`)
### 3.1 Player screen (H8, H9)
- **Segment bar:** colored blocks drawn on `ChapterSeekBar`. Red = ad (skipping), amber = unknown repeat (skipping), grey = kept intro/outro/disclaimer, outlined = restored.
- **Hold-to-mark button:** press and hold when an ad starts; it shows a live "Marking… 0:12" indicator; release when the show returns. On release, snap edges (Detection.md §6), save the Segment, search the catalog, and toast "Marked 0:45 ad · found in 6 other episodes" with Undo.
- Hold-to-mark keeps working with the screen off via an optional notification action (toggle start/stop), since holding isn't possible there.

### 3.2 Episode badge (H10)
- In episode lists and the episode screen: "3 ads dropped · 2:41". Before analysis: "Not analyzed" with an Analyze action. While analyzing: progress.

### 3.3 Lost content screen (per episode, from the badge)
- A list of every skipped range: time, duration, label, "seen in N episodes", and a play-preview button (plays the range plus 2 s on each side).
- Actions per range: **Restore here** (RESTORED_HERE), **Never skip this** (Segment action KEEP, applies to all episodes), **Relabel** (AD / INTRO / OUTRO / DISCLAIMER / BUMPER), **Edit edges** (opens the waveform editor).
- UNKNOWN_REPEAT items show a "Confirm ad" button. Confirming sets label AD and userConfirmed.
- **Find ads** button: opens the waveform editor on the whole episode to mark ads that weren't detected.

### 3.4 Waveform editor
- Draws from the stored envelope (no decoding). Pinch to zoom from the whole episode down to ~10 s. Segment overlays use the same colors as the segment bar.
- Drag the edge handles, which snap to boundary candidates unless the user holds while dragging. Tap empty space to create a new range at the nearest candidates.
- A playhead with play/pause; previewing edges plays ±3 s around a handle.
- Save → updates that Occurrence. "Apply edges to all occurrences" → recomputes the other occurrences' edges by the same offset.

### 3.5 Per-show settings (H11)
Enable AdSkip; trigger (after download / charging + idle / on demand); back-catalog N + "Analyze now"; thresholds (min episodes, min/max length); actions for INTRO/OUTRO/DISCLAIMER/BUMPER; reaction offset; per-show segment list (all Segments with counts, labels, actions, and a "share to global library" toggle).

### 3.6 Global settings (H12)
Default values for new shows; ping sound and volume; undo window length; global library (list, remove); backup folder, interval and "Back up now"; import bundle; restore; storage used plus "clear fingerprints for removed shows".

## 4. Acceptance criteria (UI-level)
- A skip lands within ±150 ms of `startMs` on a local file (measured from logs in an instrumented test).
- Undo returns to the start of the range within 1 s of the tap, and the range isn't skipped again during that session.
- Hold-to-mark → the toast appears in under 2 s for a show with 20 analyzed episodes (on the target phone).
- Every screen works in dark mode and stays readable at large font sizes.
