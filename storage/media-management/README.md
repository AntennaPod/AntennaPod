# Media Management Module

This module handles media file operations for AntennaPod, specifically file migration tasks.

## Components

### MediaFileMigrationWorker
- **Purpose**: Moves all downloaded media files to a new storage location
- **Type**: Background WorkManager worker with progress notifications
- **Features**:
  - Space validation using StorageManager API (Android 8.0+) with fallback
  - Progress notifications with percentage updates
  - Safe file copying with database updates
  - Transcript file handling
  - Download state validation and repair

### Dependencies
- `model`: Core data models (FeedMedia)
- `event`: Event system integration
- `storage/database`: Database operations (DBReader, DBWriter)
- `storage/preferences`: User preferences (UserPreferences)
- `net/download/service-interface`: File name generation
- `ui/i18n`: String resources
- `ui/notifications`: Notification channel management

## Architecture
- **Layer**: Service Layer
- **Responsibility**: Physical file operations and migration
- **Communication**: Uses regular notifications (not foreground service)
- **Error Handling**: Graceful fallback and cleanup on failures

## Usage
```java
// Enqueue migration to new path
MediaFileMigrationWorker.enqueue(context, "/new/storage/path");
```

## Notifications
- Progress notifications show migration percentage
- Completion notifications indicate success/failure
- Uses dedicated migration notification channel
