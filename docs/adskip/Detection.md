# AdSkip – Detection (`:adskip:core`)

## 1. Purpose & scope
How audio becomes skip candidates: fingerprinting, finding repeats across episodes, grouping them into segments, deciding which repeats are ads, and snapping edges. All pure Kotlin/JVM, deterministic, and unit-testable. Every number below is a **starting value to tune on real shows**, not a verified optimum.

## 2. Fingerprinting (landmark / peak-pair hashing)
Input: mono PCM at 8 kHz (float or int16), streamed in chunks.
1. **STFT:** frame 1024 samples (128 ms), hop 256 (32 ms), Hann window, magnitude in dB, bins limited to roughly 100–3 500 Hz.
2. **Peaks:** a time-frequency point that is the local maximum in a neighborhood (±~5 frames × ±~10 bins) and above an adaptive threshold (running median + margin). Target about 15–30 peaks per second.
3. **Pairs:** each anchor peak pairs with up to F=5 peaks in a target zone 1–40 frames later, within ±~60 bins.
4. **Hash:** `hash = (f1:9 bits)(f2:9 bits)(dt:6 bits)` packed into an int32. Store `(hash, anchorTimeMs)`.
5. **Envelope:** RMS in dB at 10 Hz, stored alongside.
- Output size: about 75–150 hashes/s, so 2–4 MB per hour as raw int32 pairs. If that's too big: quantize `tMs` to frame index, delta-encode, and/or reduce F.
- `fpVersion` constant: bump it when parameters change. The analysis layer re-fingerprints stale files.

## 3. Repeat discovery (new episode E vs. show index)
1. For each `(h, tE)` in E, look up `h` in the index and get `(episodeKey X, tX)`. Vote into bucket `(X, round((tX − tE)/32 ms))`.
2. **Accept a bucket** if it has ≥ M votes (start M = 20). Collect its votes' `tE` values and sort them.
3. **Extract regions:** split the bucket's `tE` list wherever the gap is > 2 s. Each run with ≥ 8 hits and duration ≥ `minSegMs` is a matched region `[tE_start, tE_end] ↔ [tX_start, tX_end]`.
4. **Self-matches:** also match E against itself (offset ≠ 0) to catch an ad played twice in one episode.
5. **Library matches:** match E against global Segments' reference hashes (only segments the user opted into global).

## 4. Clustering into Segments
- A matched region joins an existing Segment if it overlaps one of the Segment's Occurrences in the other episode X by ≥ 50% (with the matching offset applied). Otherwise it creates a new Segment with E as the reference.
- Count `episodeCount` = distinct episodes holding an Occurrence. A Segment is only "active" once `episodeCount ≥ minRepeatEpisodes`.
- When regions disagree on edges, the Segment's canonical edges are the **median** across occurrences. Each occurrence keeps its own exact edges.
- Merge two Segments if their occurrences overlap by ≥ 80% in ≥ 2 episodes (this happens when an ad is matched in two halves).

## 5. Classification (immediate skip, with protection)
Each new active Segment without a user label goes through these checks in order; the first match wins:
1. **Out of bounds:** duration < `minSegMs` or > `maxSegMs` → ignore (no Segment kept).
2. **Matches a user-labeled segment** (INTRO/OUTRO/DISCLAIMER/BUMPER/AD) → inherit that label.
3. **Structural intro/outro:** in ≥ 80% of the show's analyzed episodes AND median start within the first 120 s (→ INTRO) or median end within the last 120 s (→ OUTRO).
4. **Structural bumper:** in ≥ 80% of episodes, shorter than 15 s, at varying positions → BUMPER (segment stingers, "after the break" jingles).
5. **Disclaimer heuristic:** in ≥ 80% of episodes and adjacent (< 3 s) to an INTRO or OUTRO → DISCLAIMER.
6. **Otherwise** → UNKNOWN_REPEAT, with action **SKIP** (the user wants new repeats skipped right away). It shows up in Lost content for review.

The action for INTRO/OUTRO/DISCLAIMER/BUMPER comes from ShowSettings (default KEEP). Rules 3–5 are deliberately conservative. Recurring segment music in finance shows is the main false-positive risk, and Lost content is the safety net. Log every classification decision with its reason, so the heuristics can be tuned later.

## 6. Edge snapping (manual marks + auto edges)
Boundary candidates come from the stored 10 Hz envelope:
- **silence dips:** local minimum ≥ 12 dB below the surrounding 2 s median
- **level jumps:** |Δ median loudness over 1 s windows on each side| ≥ 4 dB
- Snap each edge to the strongest candidate within ±3 s (manual marks) or ±1 s (auto edges). If there's no candidate, keep the raw edge.
- Hold-to-mark first shifts both edges back by `reactionOffsetMs` (default 1500 ms), then snaps.

## 7. Manual mark → catalog search
1. Snap the marked span (above) and create Segment(label MANUAL_AD, action SKIP, userConfirmed).
2. Take E's hashes inside the span and query the show index (and the global library if opted in) using the same voting, with the span's hashes as the "episode".
3. Create Occurrences for every match ≥ 60% of the span's length. This applies to past episodes right away and to future ones as they're analyzed.

## 8. Skip-list merge
For a file: ACTIVE occurrences with action SKIP → sort → merge overlaps and gaps < 1 s → drop anything < 3 s after merging. Output: `List<SkipRange(startMs, endMs, segmentIds)>`.

## 9. Tests (JVM, `:adskip:core:test`)
- **Synthetic:** generate speech-like noise and music beds. Splice the same 30 s "ad" into 5 synthetic episodes at random offsets. Expect 1 Segment, 5 Occurrences, edges within ±250 ms.
- **Robustness:** the same tests after re-encoding the ad at 64/128 kbps MP3, ±3 dB gain, and with added noise (test fixtures created with ffmpeg on the dev PC and committed as small files).
- **Classification:** fixtures where a clip sits at a constant early position in all episodes → INTRO, KEEP.
- **Performance gate:** fingerprinting 1 h of PCM on the JVM in < 5 s on the dev PC. On-device timing is measured in M3.

## 10. Risks / open questions
- Hash density vs. storage: tune in M1 on the user's real shows.
- Ads re-recorded with small script changes won't match (accepted limitation).
- Shows that pitch- or speed-shift ads: not expected for podcasts. Panako-style speed-robust hashing is out of scope.
