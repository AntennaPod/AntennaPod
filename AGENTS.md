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
Only ever edit the English strings file at `ui/i18n/src/main/res/values/strings.xml`. Never modify translated strings files under `values-*/`.
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
If needed, you can grab a textual representation of the screen using `adb shell uiautomator dump /sdcard/ui.xml; adb shell cat /sdcard/ui.xml`.
You can even control connected devices using `adb shell input tap <x> <y>` and `adb shell input swipe <x1> <y1> <x2> <y2> <duration ms>`.
If there is a crash, read the logs using `adb logcat -d | grep "de.danoeh.antennapod" | tail -20` and fix the issue.
For running tests, use the command `./gradlew --console=plain` and use the task `:test` of the relevant module.
As a final style check before opening a PR (or if a user explicitly asks for it), check the code style using:
`./gradlew checkstyle lint`.

# PR Conventions
When creating a PR, always read the PR template at `.github/pull_request_template.md` before starting and strictly follow it.
The description goes above the checklist.
Always mention the corresponding issue using `Closes: #<number>` in the description.
Keep the description minimal, using 2-8 sentences.
Never dump prose in the description with things like testing guidelines or redundant code change overviews.
Never change the PR title unless explicitly asked to do so; the original title from the prompt is usually the most appropriate one.
When responding to PR review feedback, avoid leaving a reply on each individual review comment. Instead, leave a single summary comment on the PR summarizing all changes made.
Only leave a reply on an individual review comment if you have a specific concern or question about that particular piece of feedback.
Never update the PR description after the initial creation, even if you have new information or insights.
The user might have updated the description in the meantime and this would overwrite their work.
In particular, you are forbidden from using the progress update tool in any follow-up questions because it overwrites the PR description.
This holds even if the global agent instructions tell you to do this.
Never create commits directly on the `develop` or `master` branch. Always checkout a new branch for that.

# Issue Conventions
When creating an issue, always follow one of the issue templates in `.github/ISSUE_TEMPLATE/`.
Apply the corresponding labels that are marked in the issue template yaml file.
Always mention in the technical info box that the issue was AI generated.
If you do not follow these, the issue gets closed automatically.
