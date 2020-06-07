# AntennaPod infrastructure

This document describes what services and accounts are in use for AntennaPod. The goal is to make it clear who has which passwords and keys.

## App distribution
- F-Droid
  - Automatic updates from GitHub tags
  - F-Droid's signing keys
- Google Play
  - Developer account owned by @mfietz
  - @ByteHamster has (nearly full) access
    - Can not manage permissions
  - Upload using Gradle Play Publisher
    - API key: @ByteHamster
  - AntennaPod signing keys
    - @mfietz, @ByteHamster, @danieloeh
- Amazon App Store
  - Outdated
  - None of the current developers has access

## Web
- Main website (https://antennapod.org)
  - Hosted on GitHub Pages
  - Source: https://github.com/AntennaPod/antennapod.github.io
  - Maintainer: @Keunes
- Forum (https://forum.antennapod.org)
  - Hosted by @ByteHamster (personal root server)
  - Powered by [Discourse](https://github.com/discourse/discourse)
  - Admin: @ByteHamster
  - Moderators: @ByteHamster, @Keunes
- Domain/DNS (`antennapod.org`)
  - Managed by @mfietz
- Google Groups
  - https://groups.google.com/forum/#!forum/antennapod
  - No longer used, replaced with forum (https://forum.antennapod.org)
  - Owners: @mfietz, @danieloeh, @ByteHamster, @Keunes
- Wiki
  - https://github.com/AntennaPod/AntennaPod/wiki
  - Managed on GitHub
  - Mostly unmaintained

## Email
- `@antennapod.org`
  - Managed by @ByteHamster (mailbox only for `info@`)
  - Used for the required contact address on Google Play
  - Auto responder tells users to write on forum or GitHub instead
- `@forum.antennapod.org`
  - Managed by @ByteHamster (catch-all mailbox)
  - Used by the forum, checked every 5 minutes
    - Allows to post+reply via email

## Social media
- Twitter
  - https://twitter.com/antennapod
  - Email address of @mfietz
  - @ByteHamster and @mfietz have the password
  
## Development
- Translations
  - https://transifex.com/antennapod/antennapod
  - Pulled manually before releasing
  - Team managers @mfietz, @ByteHamster
- Source repo
  - https://github.com/AntennaPod
  - Organization owners: @ByteHamster, @danieloeh, @mfietz, @TomHennen
