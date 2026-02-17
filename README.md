# AntennaPod (Unofficial fork)

> **Unofficial fork – Autoplay toggle beta**
>
> This repository contains an **unofficial, unsupported** fork of [AntennaPod](https://github.com/AntennaPod/AntennaPod) maintained by **@unieng99**.  
> It is **not affiliated with, endorsed by, or supported by** the upstream AntennaPod project.

## What this fork is (and is not)

- **Purpose:** proof-of-concept work for an experimental **“Autoplay toggle”** feature.
- **Support policy:** this fork is shared **as-is**. **No support is provided.**  
  **Issues and PRs opened on this fork may be closed without action.**
- **Please don’t contact upstream about this fork:**  
  **Do not file issues, feature requests, support questions, or reviews with the upstream AntennaPod project that relate to this fork or its APKs.** Upstream maintainers did not create or review these changes and cannot troubleshoot them.
- **Upstream (official) project:** https://github.com/AntennaPod/AntennaPod  
  For the official app, documentation, and production support, use upstream.

**Current beta tag:** `v0.00.11-beta1` (APK published in this fork’s Releases)

## Branding

To avoid confusion with official AntennaPod builds, this fork uses a **different app name/package** and a **modified launcher icon** (the “UNOFF” amber badge variant). These changes are intended solely to make it clear that this is an **unofficial, experimental fork** and **not** the upstream app.

This fork also includes an **in-app notice in the About screen** stating that it is unofficial and unsupported.

---

[![GitHub check runs](https://img.shields.io/github/check-runs/AntennaPod/AntennaPod/develop)](https://github.com/AntennaPod/AntennaPod/actions/workflows/checks.yml?query=branch%3Adevelop)
[![License: GPL v3](https://img.shields.io/github/license/AntennaPod/AntennaPod)](https://www.gnu.org/licenses/gpl-3.0)
[![GitHub Release](https://img.shields.io/github/v/release/AntennaPod/AntennaPod)](https://github.com/AntennaPod/AntennaPod/releases)
[![https://img.shields.io/github/commits-since/AntennaPod/AntennaPod/latest/develop](https://img.shields.io/github/commits-since/AntennaPod/AntennaPod/latest/develop)](https://github.com/AntennaPod/AntennaPod/commits/develop/)
[![Translations on Weblate](https://hosted.weblate.org/widget/antennapod/app/svg-badge.svg?native=1)](https://hosted.weblate.org/engage/antennapod/)
[![Good first issue](https://img.shields.io/github/issues-search?query=repo%3AAntennaPod%2FAntennaPod%20is%3Aopen%20is%3Aissue%20label%3A%22Good%20first%20issue%22&label=Good%20first%20issue&labelColor=grey&color=%235F1984)](https://github.com/AntennaPod/AntennaPod/labels/Good%20first%20issue)

This fork is based on AntennaPod, the easy-to-use, flexible, open-source podcast manager for Android.

> **Important:** The badges and store links below point to the **official upstream app**, not this fork.

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png"
      alt="Get the official app on Google Play"
      height="70">](https://play.google.com/store/apps/details?id=de.danoeh.antennapod)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
      alt="Get the official app on F-Droid"
      height="70">](https://f-droid.org/app/de.danoeh.antennapod)

<img src="https://raw.githubusercontent.com/AntennaPod/StoreMetadata/main/listings/en-US/graphics/phone-screenshots/00.png" alt="AntennaPod screenshot 0 (upstream)" height="200"> <img src="https://raw.githubusercontent.com/AntennaPod/StoreMetadata/main/listings/en-US/graphics/phone-screenshots/01.png" alt="AntennaPod screenshot 1 (upstream)" height="200"> <img src="https://raw.githubusercontent.com/AntennaPod/StoreMetadata/main/listings/en-US/graphics/phone-screenshots/02.png" alt="AntennaPod screenshot 2 (upstream)" height="200"> <img src="https://raw.githubusercontent.com/AntennaPod/StoreMetadata/main/listings/en-US/graphics/phone-screenshots/03.png" alt="AntennaPod screenshot 3 (upstream)" height="200"> <img src="https://raw.githubusercontent.com/AntennaPod/StoreMetadata/main/listings/en-US/graphics/phone-screenshots/04.png" alt="AntennaPod screenshot 4 (upstream)" height="200"> <img src="https://raw.githubusercontent.com/AntennaPod/StoreMetadata/main/listings/en-US/graphics/phone-screenshots/05.png" alt="AntennaPod screenshot 5 (upstream)" height="200">

---

## Autoplay toggle beta (fork-specific)

- Adds a quick toggle to enable/disable continuous playback without changing global queue/podcast preferences.
- Queue mode respects “Follow queue” and the active queue order; Podcast mode respects per-feed autoplay when enabled.
- Built as a release variant with diagnostics off (no developer banner or debug log spam). Use the debug build only if you need verbose logging.

## Notes for upstream-savvy users

- This fork tracks upstream `develop`; only the autoplay toggle and queue auto-advance fixes are layered on top.
- **Signature mismatch vs Play/F-Droid:** you may need to uninstall the official build before installing this fork’s release APK. (Debug builds can be configured to install side-by-side if you change `applicationId`.)
- **Back up first:** export your data before switching between builds/signing keys (subscriptions, queue, playback state).

## Trying this fork

- Download the latest APK from this fork’s GitHub Releases.
- Enable “install unknown apps,” then install.
- Treat this as experimental: test on a spare device/profile if you can, and keep backups.

## Build / release this fork yourself

Build a release APK (release build type) while pointing the signing config at the standard debug keystore to avoid needing a private key:

- `./gradlew assembleRelease -PreleaseStoreFile=$HOME/.android/debug.keystore -PreleaseStorePassword=android -PreleaseKeyAlias=androiddebugkey -PreleaseKeyPassword=android`
- Output: `app/build/outputs/apk/release/app-release.apk`

Tagging/publishing:

- `git tag v0.00.11-beta1 && git push origin v0.00.11-beta1`
- Draft a GitHub Release in your fork, attach the APK, and restate the **unofficial / as-is / no support** policy.

---

## Upstream community and contributions (official project)

If you want to help the official AntennaPod project (not this fork), see:

- Forum: https://forum.antennapod.org/
- Upstream issues: https://github.com/AntennaPod/AntennaPod/issues (please read their reporting guidelines first)
- Community calls: https://forum.antennapod.org/t/monthly-community-call/1869
- Beta testing: https://antennapod.org/documentation/general/beta
- Translating: https://hosted.weblate.org/projects/antennapod/
- Building upstream: https://github.com/AntennaPod/AntennaPod/blob/develop/CONTRIBUTING.md

> **Reminder:** please do **not** use upstream channels to request support for this fork or its APKs.

## License

AntennaPod is licensed under the GNU General Public License (GPL-3.0). You can find the license text in the LICENSE file.
```
