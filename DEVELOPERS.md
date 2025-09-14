# AntennaPod Developer's Guide

Welcome to the AntennaPod developer's guide! This document provides an overview of the project structure and guidance on how to contribute, including how to use AI coding agents to enhance your development process.

## Project Architecture

AntennaPod is a large, modular Android application. The project is divided into several Gradle modules, each responsible for a specific feature or layer of the application. This modular design helps in separating concerns and makes the codebase easier to maintain and test.

Here's a high-level overview of the key modules:

-   **`app`**: The main application module that brings everything together. It contains the UI and Android-specific components.
-   **`core`**: Contains the core business logic, including database operations, feed processing, and playback management. This module is independent of the Android UI.
-   **`net`**: Handles all network-related tasks, such as downloading podcast feeds and media files.
-   **`playback`**: Manages media playback, including integration with Android's media services.
-   **`storage`**: Provides an abstraction layer for data storage, including the SQLite database.
-   **`ui`**: Contains reusable UI components and themes used across the application.
-   **`parser`**: a collection of modules for parsing feeds, media files and transcripts.

Each module has its own `build.gradle` file, where its dependencies are declared. Many modules also have their own `README.md` file with more specific details.

## Navigating the Codebase

When working on a new feature or fixing a bug, a good starting point is to identify the relevant module(s).

-   For UI-related changes, start by looking in the `app` and `ui` modules.
-   For changes to the core logic (e.g., how feeds are refreshed), look in the `core` module.
-   For networking issues, the `net` module is the place to go.

Using Android Studio's "Find in Path" feature (Ctrl+Shift+F or Cmd+Shift+F) can be very helpful for locating specific code snippets or resource strings.

## Using AI Coding Agents for Personalization

AI coding agents can be powerful tools for personalizing your AntennaPod experience or for contributing to the project. Here’s how you can effectively use them:

### What to Ask an Agent

You can ask an agent to perform a wide range of tasks, from simple bug fixes to implementing new features. Here are some examples of effective prompts:

-   **"Implement a feature that automatically stops playback after a custom time. This should be a user preference."**
-   **"Fix the bug where the podcast artwork is not displaying for some feeds. The issue is likely in the `core` or `net` module."**
-   **"Refactor the `FeedParser` class in the `parser/feed` module to improve its performance."**
-   **"Add a new screen to the app that displays user statistics, like total listening time. This will involve creating a new activity and layout in the `app` module and fetching data from the `core` module."**

### Best Practices for Working with Agents

1.  **Be Specific:** The more specific your request, the better the agent will perform. Provide as much context as possible, including the relevant modules, classes, and desired behavior.
2.  **Provide the `AGENTS.md`:** When you start a session with an agent, provide it with the `AGENTS.md` file. This will give the agent the necessary instructions to build, test, and check the code style.
3.  **Review the Agent's Plan:** Before the agent starts coding, it should provide you with a plan. Review this plan carefully to ensure it aligns with your expectations.
4.  **Monitor the Agent's Work:** Keep an eye on the agent's progress. It should be able to explain its actions and verify its changes.
5.  **Handle Errors Gracefully:** If the agent runs into an error, it should be able to debug it. If it gets stuck, you might need to provide some guidance or ask it to try a different approach.
6.  **Final Verification:** Before submitting any changes, run the tests and code style checks yourself to ensure everything is working as expected.

By following these guidelines, you can leverage AI coding agents to accelerate your development and personalization efforts on AntennaPod.

Happy coding!
