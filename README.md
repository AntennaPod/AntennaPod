# AntennaPod (Unofficial fork)

> **Unofficial, unsupported fork – unified continuous playback, improved car integration**
>
> Maintained by **@unieng99**. Not affiliated with or endorsed by the upstream [AntennaPod](https://github.com/AntennaPod/AntennaPod) project.

## Scope of this fork

- **Focus:** Experimental proof-of-concept work to support auto-advance during playback when episodes are ordered via either **(a)** the user-defined **Queue**, or **(b)** a **feed episode list** ordered by the user’s selected sort. Shared as-is. I’m not a professional Android developer.
- **How it’s enabled:** Auto-advance is controlled globally via **Settings → Playback → Playback control → Continuous playback** (formerly **Settings → Playback → Queue → Continuous playback**).
- **Queue-based continuous playback:** Triggered when you start playback from the **Queue** tab.
- **Feed-based continuous playback:** Triggered when you start playback from the **Subscriptions** tab. Example use cases include binge listening to podcasts, and sequential playback of audiobook/lecture series.
- **Support:** Distributed **as-is** with **no support**. Issues/PRs may be closed without action.
- **Upstream etiquette:** Please **do not** contact the upstream project about this fork or its APKs. The upstream maintainers did not author, review, or support these changes.

## What changed (fork highlights)

- One global continuous playback switch now governs queue/podcast auto-advance.
- In-app About screen states the build is unofficial and unsupported.

## Try it (at your own risk)

- Download the latest APK from this fork’s GitHub Releases (if published).
- You may need to uninstall the official app because signatures differ.
- Back up your data before switching between builds or signing keys.

## License

AntennaPod is licensed under the GNU General Public License (GPL-3.0). See LICENSE for details.
```
