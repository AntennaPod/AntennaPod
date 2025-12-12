# Database Maintenance Service Module

This module handles database maintenance and repair operations for AntennaPod.

## Components

### MediaRelocationWorker
- **Purpose**: Finds and updates paths for missing media files across storage locations
- **Type**: Background WorkManager worker with progress notifications
- **Features**:
  - Searches all available storage locations
  - Priority-based file matching (suffix match, then filename)
  - Download state validation and repair
  - Progress notifications with file count updates

### DatabaseMaintenanceWorker
- **Purpose**: Periodic database cleanup tasks (clearing old download logs, etc.)
- **Type**: Background WorkManager worker
- **Note**: Should never be directly triggered by users

## Dependencies
- `model`: Core data models (FeedMedia)
- `storage/database`: Database operations (DBReader, DBWriter)
- `ui/i18n`: String resources
- `ui/notifications`: Notification channel management

## Architecture
- **Layer**: Service Layer
- **Responsibility**: Database repair and maintenance operations
- **Communication**: Uses regular notifications (not foreground service)
- **Search Strategy**: Prioritizes current storage location, then searches recursively

## Usage
```java
// Enqueue media file relocation (user-triggered)
MediaRelocationWorker.enqueue(context);

// Database maintenance runs automatically (system-triggered)
```

## Notifications
- Progress notifications show checking percentage
- Completion notifications show number of relocated files
- Uses relocation notification channel integrated with main app notification system

