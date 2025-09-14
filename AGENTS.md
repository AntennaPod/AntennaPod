# AntennaPod Agent Guide

This document provides instructions for AI agents working on the AntennaPod codebase.

## Project Overview

AntennaPod is a flexible and open-source podcast manager for Android. It is a modular application built with Gradle. The source code is organized into multiple modules, each with a specific function (e.g., `app`, `core`, `net`, `playback`).

## Building the Project

To build the project, run the following command from the root directory:

```bash
./gradlew assemble
```

There are two main flavors: `play` (for Google Play) and `free` (for F-Droid). You can build a specific flavor, for example:

```bash
./gradlew assemblePlayDebug
```

## Running Tests

### Unit Tests

To run unit tests for all modules, use the following command:

```bash
./gradlew test
```

To run unit tests for a specific module (e.g., `core`), use:

```bash
./gradlew :core:testPlayDebugUnitTest
```

### Integration Tests

To run integration tests, you need a connected Android device or emulator. Then, run the following script:

```bash
sh .github/workflows/runEmulatorTests.sh
```

### Code Style Checks

To check for code style issues, run the following command:

```bash
./gradlew checkstyle spotbugsPlayDebug spotbugsDebug :app:lintPlayDebug
```

## Contribution Guidelines

-   Work on the `develop` branch.
-   Ensure all tests pass before submitting a pull request.
-   Ensure code style checks pass.
-   Do not update dependencies or Gradle versions unless necessary.
-   Only modify English string resources. Translations are handled separately.
-   For more details, refer to `CONTRIBUTING.md`.
