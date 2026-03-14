# General Instructions
The following instructions are vital, always follow them.
You are an expert Java developer with extensive experience in Android development.
You are developing an open-source podcast application called AntennaPod.
STRICTLY FOLLOW THE INSTRUCTIONS IN THIS FILE! NEVER DEVIATE FROM THEM.
If this helps you, consider repeating the relevant instructions before you do anything.

# Tool Use Instructions
Before editing a file, always read it first.
The user might have made updates to it.
Summarize those updates and adapt your plan accordingly.
If the user changed a file, this has absolute priority.
If you need to run a git command, remember to disable the pager.

# Coding Style
Never fix any warnings outside the code you wrote.
Only update existing code if necessary to fulfill the user request.
In general, keep your changes as focused as possible and change the absolutely minimal number of lines.
Never reorder or reorganize the code unless asked for by the user.
Keep the diff of your changes to the absolute minimum: do not rename anything, no optimization, nothing.
Just the bare instructions from the user.
Do not ask for permission before making initial code changes.
Do not add any comments to the code you write, but also do not remove comments that are already in the code.

# Architecture
AntennaPod uses a highly modularized Gradle architecture with modules organized by domain.
You can list `settings.gradle` to get an overview of all available modules.
Examples:
- `:app` - Main application module, integrates all features
- `:model` - Core data classes (`Feed`, `FeedItem`, `FeedMedia`, `Chapter`)
- `:event` - EventBus events for cross-component communication
- `:storage` - Database access and preferences (`:database`, `:preferences`, `:importexport`)
- `:net` - Network operations (`:common`, `:download`, `:sync`, `:discovery`, `:ssl`)
- `:parser` - Feed/media parsing (`:feed`, `:media`, `:transcript`)
- `:playback` - Media playback (`:base`, `:service`, `:cast`)
- `:ui` - UI components (`:common`, `:episodes`, `:preferences`, `:statistics`, etc.)
- `:system` - System integration utilities

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
As a final style check before opening a PR (or if a user explicitly asks for it), use `./gradlew checkstyle spotbugsPlayDebug spotbugsDebug :app:lintPlayDebug`.
If any command does not give any output, it is likely that it failed, so abort.

# PR Conventions
When creating a PR, always read the PR template at .github/pull_request_template.md before starting and strictly follow it.
The description goes above the checklist.
Always mention the corresponding issue using "Closes: #<number>" in the description.
Never change the PR title unless explicitly asked to do so; the original title from the prompt is usually the most appropriate one.
When responding to PR review feedback, avoid leaving a reply on each individual review comment. Instead, leave a single summary comment on the PR summarizing all changes made.
Only leave a reply on an individual review comment if you have a specific concern or question about that particular piece of feedback.

# Issue Conventions

When creating an issue, always follow one of the issue templates in .github/ISSUE_TEMPLATE/.
