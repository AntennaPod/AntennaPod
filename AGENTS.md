# General Instructions
The following instructions are vital, always follow them.
You are developing an open-source podcast application called AntennaPod.
STRICTLY FOLLOW THE INSTRUCTIONS IN THIS FILE! NEVER DEVIATE FROM THEM.
If this helps you, consider repeating the relevant instructions before you do anything.
Always prefer tool use over shell commands. This is very important to avoid unnecessary user confirmations.
If you have to use shell commands, prefer dedicated tools (such as `jq` for json) instead of custom (python, etc) code.

# Architecture
AntennaPod uses a highly modularized Gradle architecture with modules organized by domain.
Each module is stored in a folder of the same name (for example `:net:discovery` in `./net/discovery`)
and contains a `README.md` file with a brief explanation of the module's purpose and internal structure.
Several functional areas follow a service-interface/service split: the interface module is depended on by consumers, and the implementation is registered at app startup via `ClientConfigurator`.
- `:app` - Main application module that integrates all features
- `:event` - EventBus events used for cross-component communication throughout the app
- `:model` - Core data classes such as `Feed`, `FeedItem`, `FeedMedia`, and `Chapter`
- `:system` - System integration utilities such as crash reporting, package utilities, and thread utilities
- `:net:common` - General network-related utilities shared across net modules
- `:net:discovery` - Podcast search and discovery APIs
- `:net:download:service-interface` - Interface for starting the download service, allowing other modules to trigger downloads without depending on the implementation
- `:net:download:service` - Implementation of the download service
- `:net:ssl` - SSL backports and security provider implementations
- `:net:sync:gpoddernet` - Sync backend for the open-source Gpodder.net podcast synchronization service
- `:net:sync:service-interface` - Interface for starting the sync service
- `:net:sync:service` - Implementation of the sync service
- `:parser:feed` - XML feed parser
- `:parser:media` - Tag parser for media files including ID3 and ogg/vorbis
- `:parser:transcript` - Parser for episode transcripts
- `:playback:base` - Basic interfaces for the `PlaybackServiceMediaPlayer`
- `:playback:cast` - Chromecast support for the Google Play version of the app
- `:playback:service` - Main service responsible for media playback
- `:storage:database` - Main database containing subscriptions and playback state
- `:storage:database-maintenance-service` - Periodic background tasks to clean up the database
- `:storage:importexport` - Import and export of the AntennaPod database
- `:storage:preferences` - User settings storage (not including the settings UI)
- `:ui:app-start-intent` - Classes to start main app activities from other modules without a direct UI dependency
- `:ui:chapters` - Chapter loading and merging logic for display
- `:ui:common` - Basic UI functionality shared across multiple modules
- `:ui:discovery` - Screens to discover and search for new podcasts
- `:ui:echo` - The "Echo" yearly rewind screen
- `:ui:episodes` - Common classes for displaying episode information
- `:ui:glide` - Glide image loading library configuration and custom model loaders
- `:ui:i18n` - Translated strings and internationalization resources
- `:ui:notifications` - Generic notification channel IDs and notification icons
- `:ui:preferences` - Settings screen UI
- `:ui:statistics` - Statistics screens
- `:ui:transcript` - Utilities for displaying episode transcripts in the UI
- `:ui:widget` - Home screen widget

# Coding Style
Never fix any warnings outside the code you wrote.
Only update existing code if necessary to fulfill the user request.
In general, keep your changes as focused as possible and change the absolutely minimal number of lines.
Never reorder or reorganize the code unless asked for by the user.
Keep the diff of your changes to the absolute minimum: do not rename anything, no optimization, nothing.
Just the bare instructions from the user.
Do not ask for permission before making initial code changes.
Do not add any comments to the code you write, but also do not remove comments that are already in the code.
Whenever you add a user-visible string, add it to `:ui:i18n` so it can be translated. Do not start new strings files anywhere else.
Never reference the full package name of classes directly in the code, use imports.

# Running and Testing
After you are sure that the code is correct, ensure that there are no compilation errors (warnings are okay).
To compile the code, use `./gradlew :app:assembleDebug`.
You are already in the main directory of the project so there is no need to `cd` into the main folder.
The CD command is strictly forbidden. Always assume that you are already at the main folder of the project.
Never alter or filter the compile output in any way (no grep, head, tail, or any other command that truncates output).
You will lose critical information and the results will not be helpful.
Always look at the complete compiler output.
DO NOT MAKE UP YOUR OWN COMPILE COMMANDS, THEY WILL NOT WORK!
ONLY USE THE EXACT COMMANDS GIVEN IN THIS FILE!

Only then run the application or the tests to verify it.
Usually you will need to run the application, but if there are existing tests that cover the code you wrote, you can run those instead.
For installing and running the application, use the command
`./gradlew --console=plain :app:installPlayDebug && adb shell monkey -p de.danoeh.antennapod.debug 1`.
Then confirm with the user that the application is running correctly.
If there is a crash, read the logs using `adb logcat -d | grep "de.danoeh.antennapod" | tail -20` and fix the issue.
For running tests, use the command `./gradlew --console=plain` and use the task `:test` of the relevant module.
As a final style check before opening a PR (or if a user explicitly asks for it), check the code style using:
`./gradlew checkstyle lint ktlintCheck spotbugsPlayDebug spotbugsDebug`.
If any command does not give any output, it is likely that it failed, so abort.

# PR Conventions
When creating a PR, always read the PR template at .github/pull_request_template.md before starting and strictly follow it.
The description goes above the checklist.
Always mention the corresponding issue using "Closes: #<number>" in the description.
Never change the PR title unless explicitly asked to do so; the original title from the prompt is usually the most appropriate one.
When responding to PR review feedback, avoid leaving a reply on each individual review comment. Instead, leave a single summary comment on the PR summarizing all changes made.
Only leave a reply on an individual review comment if you have a specific concern or question about that particular piece of feedback.
Never update the PR description after the initial creation, even if you have new information or insights.
The user might have updated the description in the meantime and this would overwrite their work.
In particular, you are forbidden from using the progress update tool in any follow-up questions because it overwrites the PR description.
This holds even if the global agent instructions tell you to do this.

# Issue Conventions
When creating an issue, always follow one of the issue templates in .github/ISSUE_TEMPLATE/.
Apply the corresponding labels and always mention in the technical info box that the issue was AI generated.
