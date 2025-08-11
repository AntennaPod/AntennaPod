# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Building the App
- `./gradlew assemblePlayDebug` - Build debug APK for Play Store variant
- `./gradlew assembleFreeDebug` - Build debug APK for F-Droid variant
- `./gradlew build` - Build all variants

### Code Quality and Linting
- `./gradlew checkstyle spotbugsPlayDebug spotbugsDebug :app:lintPlayDebug` - Run all code quality checks
- `./gradlew checkstyle` - Run Checkstyle code style checks
- `./gradlew spotbugsPlayDebug` - Run SpotBugs static analysis

### Testing
- `./gradlew :core:testPlayDebugUnitTest` - Run unit tests for core module
- `sh .github/workflows/runTests.sh` - Run integration tests (requires AVD or device)
- Individual module tests: `./gradlew :MODULE_NAME:test`

## Project Architecture

AntennaPod follows a modular Android architecture with clear separation of concerns:

### Core Modules
- **:app** - Main application module containing Activities, Fragments, and UI logic
- **:model** - Core data models (Feed, FeedItem, Chapter, etc.)
- **:storage** - Data persistence layer with database, preferences, and import/export
  - `:storage:database` - Room database implementation
  - `:storage:preferences` - SharedPreferences wrapper
  - `:storage:importexport` - OPML and data export/import functionality

### Network Layer (:net)
- **:net:common** - Shared networking utilities
- **:net:discovery** - Podcast feed discovery and search
- **:net:download** - Download management with service interface and implementation
- **:net:ssl** - SSL certificate handling
- **:net:sync** - Synchronization with services like gpodder.net

### Media & Parsing (:parser)
- **:parser:feed** - RSS/Atom feed parsing
- **:parser:media** - Audio/video metadata extraction
- **:parser:transcript** - Podcast transcript parsing

### Playback (:playback)
- **:playback:base** - Core playback interfaces and utilities
- **:playback:service** - Background playback service
- **:playback:cast** - Chromecast support (Play Store variant only)

### UI Modules (:ui)
- **:ui:common** - Shared UI components, themes, and utilities
- **:ui:episodes** - Episode list views and adapters
- **:ui:preferences** - Settings screens and dialogs
- **:ui:statistics** - Usage statistics and charts
- **:ui:notifications** - Playback notifications
- **:ui:widget** - Home screen widget
- **:ui:i18n** - Internationalization strings
- **:ui:echo** - Echo RSS recommendations feature

### Key Application Classes
- `PodcastApp` - Main Application class with initialization
- `MainActivity` - Primary activity with navigation drawer
- `ClientConfigurator` - Global configuration setup
- EventBus architecture for component communication

### Build Variants
- **Play** - Google Play Store version with Google Cast support
- **Free** - F-Droid version without proprietary dependencies

### Testing Strategy
- Unit tests in each module's `src/test` directory
- Integration tests in `:app:src/androidTest`
- Use Robolectric for Android unit tests
- Espresso for UI automation tests

## Development Guidelines

### Code Style
- Follow existing Java/Android conventions
- Use the provided Checkstyle configuration (`config/checkstyle/checkstyle.xml`)
- Only modify English string resources - translations handled via Weblate

### Module Dependencies
- UI modules depend on storage, network, and model modules
- Storage modules are independent of UI
- Network modules handle all external communication
- Parser modules process external data formats

### Common Development Patterns
- EventBus for cross-component communication
- RxJava for asynchronous operations
- Glide for image loading
- Room for database operations
- Material Design components for UI