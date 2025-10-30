How to report a bug
-------------------
- Before anything else, please make sure you are on the latest version, the bug you are experiencing may have been fixed already!
- Use the search function to see if someone else has already submitted the same bug report.
- Try to describe the problem with as much detail as possible.
- Some bugs may only occur on certain devices or versions of Android. Please add information about your device and the version of Android that is running on it (you can look these up under `Settings → About Phone`), as well as which version of AntennaPod you are using.
- If the bug only seems to occur with a certain podcast, please include the URL of that podcast.
- If possible, add instructions on how to reproduce the bug.
- If possible, add a logfile to your post. This is especially useful if the bug makes the application crash. AntennaPod has an `export logs` feature for this.
- Usually, you can take a screenshot of your smartphone by pressing *Power* + *Volume down* for a few seconds.
- Please use the following **[template](https://github.com/AntennaPod/AntennaPod/issues/new?assignees=&labels=Type%3A+Possible+bug&template=bug_report.yml)**.


How to submit a feature request
-------------------------------
- Make sure you are using the latest version of AntennaPod. Perhaps the feature you are looking for has already been implemented.
- Use the search function to see if someone else has already submitted the same feature request. If there is another request already, please upvote the first post instead of commenting something like "I also want this".
- To make it easier for us to keep track of requests, please only make one feature request per issue.
- Give a brief explanation about the problem that may currently exist and how your requested feature solves this problem.
- Try to be as specific as possible. Please not only explain what the feature does, but also how. If your request is about (or includes) changing or extending the UI, describe what the UI would look like and how the user would interact with it.
- Please use the following **[template](https://github.com/AntennaPod/AntennaPod/issues/new?assignees=&labels=&template=feature_request.yml)**. 


Translating AntennaPod
----------------------
If you would like to translate the app into another language or improve an existing translation, you can visit the [Transifex project page](https://www.transifex.com/antennapod/antennapod/). From there, you can either join a language team if it already exists or create a new language team.


Submit a pull request
---------------------
- Before you work on the code
  - Make sure that there is an issue *without* the `Needs: Triage` or `Needs: Decision` label for the feature you want to implement or bug you want to fix. If you just start working on a feature that is not approved yet (or not even has an issue), your PR might not get merged.
  - Add a comment to the issue so that other people know that you are working on it.
    - You don't need to ask for permission to work on something, just indicate that you are doing so.
  - If you want to discuss the approach to take, feel free to ask in the issue or join a [community call](https://antennapod.org/events/community-meeting).
- Fork the repository
- Create a new branch for your contribution
  - This makes opening possible additional pull requests easier.
  - As a base, use the `develop` branch.
    - Almost all changes of AntennaPod are done on the `develop` branch. If a new version of AntennaPod is released, the `develop` branch is merged into `master`. As a result, the `master` branch probably doesn't contain the latest changes when you are reading this. Otherwise, there might be a lot of merge-conflicts when merging your changes into `develop` and therefore it might take longer to review your pull-request.
- Get coding :)
  - If possible, add unit tests for your pull request and make sure that they pass.
  - Please do not upgrade dependencies or build tools unless you have a good reason for it. Doing so can easily introduce bugs that are hard to track down.
  - Please follow our code style. You can use Checkstyle within Android Studio using our [configuration file](https://github.com/AntennaPod/AntennaPod/blob/develop/config/checkstyle/checkstyle.xml).
  - To check the code style locally, run `./gradlew checkstyle spotbugsPlayDebug spotbugsDebug :app:lintPlayDebug`
  - Please only change the English string resources. Translations are handled on [Transifex](https://www.transifex.com/antennapod/antennapod/).
- Open the PR
  - Mention the corresponding issue in the pull request text, so that it can be closed once your pull request has been merged. If you use [special keywords](https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue), GitHub will close the issue(s) automatically.


Building From Source
--------------------------
1. Fork this repository
1. Download Android Studio
1. Download AntennaPod
   1. Option A: Using the git command line (recommended)
       1. Use `git clone <url>` with the remote url of your forked repo.
          The AntennaPod repo contains a large submodule with app store metadata like screenshots.
          You **do not need that** for normal development.
       1. In Android Studio: File » New » Project from existing sources
   1. Option B: From Android Studio
       1. File » New » Project from version control
       1. Enter the remote url of the forked repo
1. Wait for a long time until all progress bars go away
1. Press the Play button

Testing and Verifying
--------------------------
As a developer contributing to AntennaPod, we ask that you test the feature yourself manually and better yet, add unit and functional tests to any feature of bug you fix.

### Running Unit Tests
* `./gradlew :core:testPlayDebugUnitTest`

### Running Integration Tests

#### Using Android Studio
* Create a configuration via 'Run->Edit Configurations...'

<img width="768" alt="antennapod-run-tests"
src="https://user-images.githubusercontent.com/149837/105122859-e1317180-5a8b-11eb-8d45-d54a3b051a9b.png">

#### Using the command line
* Start an AVD or plug in your phone
* `sh .github/workflows/runTests.sh`
