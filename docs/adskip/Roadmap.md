# AdSkip – Roadmap

Each milestone gets its own branch (`adskip/mN-name`) and must leave the app building and usable. **M1 proves detection works on the user's real shows on a PC before any Android work.**

| # | Milestone | Deliverables | Acceptance criteria |
|---|---|---|---|
| M0 | Fork setup | GitHub fork; default branch `main` from upstream `develop`; own `applicationId`, name, icon (H3, H15); user agent (H13); own PodcastIndex key via Gradle property (H14); `minSdk 31` (H2); empty `:adskip:*` modules wired in (H1); `docs/adskip/` committed; `CLAUDE.md` imports `AGENTS-adskip.md`; new `README.md` (what it is, Based on AntennaPod, GPL-3.0, no Play Store); CI workflow building the `free` debug APK | `./gradlew :app:assembleDebug` passes in CI; installs next to stock AntennaPod; no "AntennaPod" in launcher name, About title, or user agent |
| M1 | Core engine + CLI prototype | `:adskip:core` fingerprint, findRepeats, cluster, skip-list merge; JVM tests from Detection.md §9; minimal `:adskip:cli analyze --dir` printing segments as a table + Audacity label files | Synthetic tests pass; on 10 real episodes of one of the user's shows, the user checks the label files in Audacity: ≥ 90% of baked-in repeated ads found, edges ±1 s, intros not marked AD |
| M2 | Storage | Room DB + entities, episode identity, fp/env blob store, `fpVersion` handling | Unit tests: write/read/merge; stale-file detection when the file fingerprint changes |
| M3 | Android analysis | MediaCodec decode → 8 kHz mono stream; WorkManager job; three triggers; back-catalog N; hook H4 | 1 h episode analyzed on the target phone; log decode + fingerprint time (target < 60 s); memory < 100 MB peak |
| M4 | Classification | Detection.md §5 rules + reason logging; ShowSettings defaults | Fixture tests for INTRO/OUTRO/BUMPER/DISCLAIMER; on real data, intros kept |
| M5 | Skip playback | Hooks H5–H7; PlayerMessage controller; ping; undo snackbar + media-session action; user-seek suppression; cast fallback | Criteria in PlaybackAndUI.md §4 (timing, undo) |
| M6 | Badge + segment bar + Lost content | H8, H10; Lost content screen with preview, restore, never skip, relabel, confirm | Restoring makes the range play next time; "Never skip" applies to all episodes |
| M7 | Hold-to-mark | Button (H9), reaction offset, snapping, catalog search, notification toggle | Toast < 2 s; marked ad found in other episodes that contain it |
| M8 | Waveform editor | Envelope rendering, zoom, snapping handles, preview, apply-to-all | Edges adjustable to ±100 ms; saved edges used by the skip controller |
| M9 | Settings + global library | Per-show (H11) and global (H12) settings; share-to-global; global matching in the analysis job | Shared sponsor clip detected in a second show |
| M10 | Backup/restore | Periodic SAF zip backup (keep 3), restore with merge rules | Wipe app data → restore → labels and skips return |
| M11 | PC bulk tool | `:adskip:cli analyze --feed/--dir` → bundle.zip; phone import | Import seeds Segments; phone analysis of the same show then needs no manual marking for the imported ads |
| M12 | Polish & performance | Profile on device; move FFT/peaks to native code only if M3 times miss the target; battery check; storage cleanup | 10 episodes analyzed while charging overnight with no user-visible issues |

## Releases (after M6, when it's usable)
- Signed `freeRelease` APK built by a GitHub Actions workflow from tags (`v0.x.y`). Keystore and passwords stored as Actions secrets. Attach the APK to a GitHub Release.
- Users install with Obtainium or by downloading the APK. Consider submitting to F-Droid (or a self-hosted F-Droid repo) once it's stable.
- Every release's source is the tagged commit (satisfies the GPL source requirement).

## Upstream maintenance
- About monthly: merge upstream `develop`, re-check hooks H1–H12, run `:adskip:*:test` and a manual playback smoke test.

## Open questions (decide during the build)
1. FFT: hand-written vs. JTransforms (decide by the M1 benchmark).
2. Hash density / storage per hour (tune in M1).
3. Whether "download for analysis" should auto-download the back catalog or always ask (default: ask).
