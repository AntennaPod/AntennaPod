# AdSkip fork – agent rules

This repo is a public, GPL-3.0 fork of AntennaPod, renamed (working name: **AdSkip**). It adds on-device audio fingerprinting that finds ads repeated across a show's episodes and skips them during playback. It is distributed via GitHub Releases (and later F-Droid), never Google Play. It is not submitted upstream.

Read these before working:
- `docs/adskip/Architecture.md`: modules, hook points, data model, backup, PC tool
- `docs/adskip/Detection.md`: fingerprinting, repeat discovery, classification, edge snapping
- `docs/adskip/PlaybackAndUI.md`: skipping, ping/undo, screens, hold-to-mark
- `docs/adskip/Roadmap.md`: milestones and acceptance criteria. Work one milestone at a time.

## Rules that override or extend AGENTS.md
1. **Keep AdSkip code in the `:adskip:*` modules.** Upstream files may only get the small hooks listed in Architecture.md §4. If a change needs a new hook, add it to that list in the same commit.
2. **Never change upstream's database schema** (`PodDBAdapter`, `DBUpgrader`). AdSkip has its own database (Architecture.md §5).
3. **Strings:** AdSkip strings live in `adskip/ui/src/main/res/values/strings.xml`, not in `:ui:i18n`. This overrides the AGENTS.md rule to keep upstream merges clean. Hooks in upstream UI files reference AdSkip strings through the `:adskip:ui` module.
4. **Language:** New modules use Kotlin. Hooks in upstream files stay Java, in upstream style.
5. **`:adskip:core` is pure Kotlin/JVM** with no Android imports, so it unit-tests on the JVM and is reused by the PC tool.
6. **Never open issues or PRs against AntennaPod/AntennaPod.** The PR/issue conventions in AGENTS.md apply to *this* repo only where they make sense. Always target this repo: `gh repo set-default <owner>/<fork>` is set, and `gh pr create` must pass `--repo <owner>/<fork>`. Work on branches named `adskip/<milestone>` and PR them into this repo's `main`.
7. **Branding:** never use the AntennaPod name, logo, or wordmark in user-visible UI, store text, or the app ID. Keep upstream copyright notices, the GPL license, and the "Based on AntennaPod" credit on the About screen.
8. **Secrets:** never commit signing keys, keystore passwords, or API keys. The PodcastIndex key/secret and the signing config come from Gradle properties / CI secrets.
9. **Build/test commands:** use the exact ones in AGENTS.md, plus `./gradlew --console=plain :adskip:core:test` (and `:test` for other adskip modules).
10. **No network calls from AdSkip** except the PC tool downloading RSS enclosures the user asked for.
