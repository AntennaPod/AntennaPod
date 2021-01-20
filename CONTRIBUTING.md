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
- Please use the following **[template](.github/ISSUE_TEMPLATE/bug_report.md)**.


How to submit a feature request
-------------------------------
- Make sure you are using the latest version of AntennaPod. Perhaps the feature you are looking for has already been implemented.
- Use the search function to see if someone else has already submitted the same feature request. If there is another request already, please upvote the first post instead of commenting something like "I also want this".
- To make it easier for us to keep track of requests, please only make one feature request per issue.
- Give a brief explanation about the problem that may currently exist and how your requested feature solves this problem.
- Try to be as specific as possible. Please not only explain what the feature does, but also how. If your request is about (or includes) changing or extending the UI, describe what the UI would look like and how the user would interact with it.


Translating AntennaPod
----------------------
If you would like to translate the app into another language or improve an existing translation, you can visit the [Transifex project page](https://www.transifex.com/antennapod/antennapod/). From there, you can either join a language team if it already exists or create a new language team.


Submit a pull request
---------------------
- If you want to work on a feature that has been requested or fix a bug that has been reported on the "issues" page, add a comment to it so that other people know that you are working on it.
- Fork the repository.
- Almost all changes of AntennaPod are done on the `develop` branch. If a new version of AntennaPod is released, the `develop` branch is merged into `master`. As a result, the `master` branch probably doesn't contain the latest changes when you are reading this. Please make sure that you are branching from `develop`! Otherwise, there might be a lot of merge-conflicts when merging your changes into `develop` and therefore it might take longer to review your pull-request. Exceptions are urgent issues that need to be fixed in the production version.
- If your pull request fixes a bug that has been reported or implements a feature that has been requested in another issue, try to mention it in the message, so that it can be closed once your pull request has been merged. If you use special keywords in the [commit comment](https://help.github.com/en/github/managing-your-work-on-github/linking-a-pull-request-to-an-issue) or [pull request text](https://github.blog/2013-05-14-closing-issues-via-pull-requests/), GitHub will close the issue(s) automatically.
- If possible, add unit tests for your pull request and make sure that they pass.
- Please do not upgrade dependencies or build tools unless you have a good reason for it. Doing so can easily introduce bugs that are hard to track down.
- If you plan to do a change that touches many files (10+), please ask beforehand. This usually causes merge conflicts for other developers.
- Please follow our code style. You can use Checkstyle within Android Studio using our [coniguration file](https://github.com/AntennaPod/AntennaPod/blob/develop/config/checkstyle/checkstyle-new-code.xml).


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
