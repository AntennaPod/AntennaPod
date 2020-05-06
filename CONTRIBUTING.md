How to report a bug
-------------------
- Use the search function to see if someone else has already submitted the same bug report.
- Try to describe the problem with as much detail as possible.
- Some bugs may only occur on certain devices or versions of Android. Please add information about your device and the version of Android that is running on it (you can look these up under `Settings → About Phone`), as well as which version of AntennaPod you are using.
- If the bug only seems to occur with a certain podcast, please include the URL of that podcast.
- If possible, add instructions on how to reproduce the bug.
- If possible, add a logfile to your post. This is especially useful if the bug makes the application crash. You can create logfiles with apps like `aLogcat`. Just launch `alogcat`, then start AntennaPod and reproduce the bug. aLogcat should now display information about the bug when you start it. AntennaPod also has an `export logs` feature.
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
- Almost all changes of AntennaPod are done on the `develop` branch. If a new version of AntennaPod is released, the `develop` branch is merged into `master`. As a result, the `master` branch probably doesn't contain the latest changes when you are reading this. Please make sure that you are branching from `develop`! Otherwise, there might be a lot of merge-conflicts when merging your changes into `develop` and therefore it might take longer to review your pull-request. Exceptions are changes in files like README.md, CONTRIBUTING.md, and other files that are not directly related to the source code.
- If your pull request fixes a bug that has been reported or implements a feature that has been requested in another issue, try to mention it so that it can be closed once your pull request has been merged. If you use special keywords in the [commit comment](https://help.github.com/en/github/managing-your-work-on-github/linking-a-pull-request-to-an-issue) or [pull request text](https://github.blog/2013-05-14-closing-issues-via-pull-requests/), GitHub will close the issue(s) automatically when the changes land on the master branch.
- Although not every part of AntennaPod's source code is fully documented yet, it would be very nice if you could add documentation to your changes if it is a larger pull request.
- If possible, add unit tests for your pull request and make sure that they pass. Information on how to add unit tests and run them can be found here: (TODO)


Improving this file
-------------------
If you think this file needs clarification or additional information on certain topics, feel free to improve it via pull requests or by opening an issue.
