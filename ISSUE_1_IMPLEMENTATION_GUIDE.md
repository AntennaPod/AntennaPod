# Issue 1: DBWriter Integration - Detailed Implementation Guide

**Issue**: QueueRepositoryImpl uses custom ExecutorService instead of DBWriter's shared executor
**Scope**: Move 9 async queue operations to DBWriter
**Estimated Effort**: 8-10 hours
**Risk Level**: HIGH (threading is complex, must be correct)

---

## Overview

**Problem**: QueueRepositoryImpl lines 43-50 create a custom ExecutorService:
```java
private static final ExecutorService dbExec;
static {
    dbExec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setName("QueueDatabaseExecutor");
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });
}
```

This creates a **separate thread** from the main database thread, violating AntennaPod's single-threaded database access principle.

**Solution**: Use DBWriter's existing `dbExec` instead, following the established pattern.

---

## Operations to Move (9 total)

| # | Method | Current Location | New Location |
|---|--------|------------------|--------------|
| 1 | createQueue() | QueueRepositoryImpl:100-159 | DBWriter (new) |
| 2 | updateQueue() | QueueRepositoryImpl:172-213 | DBWriter (new) |
| 3 | deleteQueue() | QueueRepositoryImpl:226-251 | DBWriter (new) |
| 4 | switchActiveQueue() | QueueRepositoryImpl:265-327 | DBWriter (new) |
| 5 | addEpisodeToQueue() | QueueRepositoryImpl:376-404 | DBWriter (new) |
| 6 | removeEpisodeFromQueue() | QueueRepositoryImpl:417-449 | DBWriter (new) |
| 7 | reorderQueueEpisodes() | QueueRepositoryImpl:553-596 | DBWriter (new) |
| 8 | clearQueue() | QueueRepositoryImpl:498-525 | DBWriter (new) |
| 9 | moveEpisodeBetweenQueues() | QueueRepositoryImpl:365-404 | DBWriter (new) |

---

## Step-by-Step Implementation

### Step 1: Add Queue Methods to DBWriter

**File**: `storage/database/src/main/java/de/danoeh/antennapod/storage/database/DBWriter.java`

#### 1a. Add Queue-related imports
```java
import de.danoeh.antennapod.model.feed.Queue;
import de.danoeh.antennapod.event.QueueEvent;  // Use for queue events
```

#### 1b. Add createQueue() to DBWriter
```java
public static Future<Long> createQueue(@NonNull final Queue queue) {
    return dbExec.submit(() -> {
        // Copy implementation from QueueRepositoryImpl (lines 108-156)
        // This becomes the single source of truth

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            // Insert queue
            long queueId = adapter.insertQueue(queue);
            queue.setId(queueId);

            // Post event
            EventBus.getDefault().post(new QueueEvent(/*...*/));

            return queueId;
        } finally {
            adapter.close();
        }
    });
}
```

**Pattern**: Each DBWriter method:
- Takes parameters needed for operation
- Returns `Future<?>` (or `Future<Long>` for ID-returning operations)
- Uses `dbExec.submit()`
- Handles adapter open/close
- Posts appropriate event via EventBus
- Includes error handling with logging

#### 1c. Add remaining 8 methods following same pattern
- updateQueue()
- deleteQueue()
- switchActiveQueue()
- addEpisodeToQueue()
- removeEpisodeFromQueue()
- reorderQueueEpisodes()
- clearQueue()
- moveEpisodeBetweenQueues()

### Step 2: Modify QueueRepositoryImpl to Delegate

**File**: `storage/database/src/main/java/de/danoeh/antennapod/storage/database/QueueRepositoryImpl.java`

#### 2a. Remove ExecutorService declaration
Delete lines 43-50:
```java
// DELETE THIS ENTIRE BLOCK
private static final ExecutorService dbExec;
static {
    dbExec = Executors.newSingleThreadExecutor(...);
}
```

#### 2b. Remove import statement
Delete: `import java.util.concurrent.ExecutorService;`
Delete: `import java.util.concurrent.Executors;`

#### 2c. Replace each method with delegation
Instead of `return dbExec.submit(...)`, call DBWriter method:

```java
// OLD
@Override
public Future<Long> createQueue(@NonNull Queue queue) {
    return dbExec.submit(() -> {
        // ... implementation
    });
}

// NEW
@Override
public Future<Long> createQueue(@NonNull Queue queue) {
    return DBWriter.createQueue(queue);
}
```

Apply this pattern to all 9 methods.

#### 2d. Update imports
Remove async-related imports, keep only what's needed for delegation.

### Step 3: Handle EventBus Posting

**Important**: When moving code from QueueRepositoryImpl to DBWriter, EventBus posts might need adjustment.

**Current pattern in QueueRepositoryImpl**:
```java
// Posts QueueSwitchedEvent or QueueContentChangedEvent
EventBus.getDefault().post(new QueueSwitchedEvent(/*...*/));
```

**DBWriter pattern**:
```java
// Posts QueueEvent with action type
EventBus.getDefault().post(new QueueEvent(/*...*/));
```

**Action**: When copying code, align event posting with DBWriter's approach (use QueueEvent instead of custom events).

### Step 4: Handle Error Cases

**Key consideration**: Error handling must be consistent.

**Pattern**:
```java
try {
    adapter.open();
    // Do work
    return result;
} catch (Exception e) {
    Log.e(TAG, "Error doing operation", e);
    throw new RuntimeException(e); // or handle appropriately
} finally {
    adapter.close();
}
```

### Step 5: Test Each Migration

**After moving each method**:

1. **Compile check**:
   ```bash
   ./gradlew :storage:database:compilePlayDebugJavaWithJavac
   ```

2. **Run queue tests**:
   ```bash
   ./gradlew :storage:database:testPlayDebugUnitTest --tests "Queue*"
   ```

3. **Expected result**: All 23 queue tests still pass (QueueDaoTest + QueueRepositoryImplTest)

### Step 6: Verify No Double Threading

**After all migrations complete**:

1. Verify no remaining `ExecutorService` in QueueRepositoryImpl
2. Verify all async methods delegate to DBWriter
3. Search for any remaining `dbExec` references in queue code:
   ```bash
   grep -r "dbExec" storage/database/src/ | grep -i queue
   ```
   Should return: nothing

4. Verify tests still pass:
   ```bash
   ./gradlew :storage:database:testPlayDebugUnitTest --tests "Queue*"
   ```

---

## Implementation Sequence

Recommended order (by complexity/dependencies):

1. ✅ **Add imports to DBWriter**
2. ✅ **Add readQueue() methods** (getActiveQueue, getQueueById) - already synchronous, might not need migration
3. ✅ **Add createQueue()** - Simple, no dependencies
4. ✅ **Add updateQueue()** - Straightforward queue update
5. ✅ **Add deleteQueue()** - Queue deletion
6. ✅ **Add switchActiveQueue()** - Complex, with cleanup logic
7. ✅ **Add addEpisodeToQueue()** - Episode operations
8. ✅ **Add removeEpisodeFromQueue()** - Episode operations
9. ✅ **Add reorderQueueEpisodes()** - Reordering
10. ✅ **Add clearQueue()** - Clear operation
11. ✅ **Add moveEpisodeBetweenQueues()** - Complex episode move
12. ✅ **Update QueueRepositoryImpl** - Replace all async methods with delegation
13. ✅ **Remove ExecutorService** from QueueRepositoryImpl
14. ✅ **Run all tests** - Verify no regressions

---

## Complexity Analysis

### High Complexity
- **switchActiveQueue()**: Handles queue switch + auto-cleanup + event posting
- **moveEpisodeBetweenQueues()**: Complex episode move with duplicate handling

### Medium Complexity
- **deleteQueue()**: Queue deletion with cascade cleanup
- **createQueue()**: Creates new queue and potentially manages defaults

### Low Complexity
- **addEpisodeToQueue()**, **removeEpisodeFromQueue()**: Single episode operations
- **clearQueue()**: Clear all episodes from queue
- **reorderQueueEpisodes()**: Reorder episodes
- **updateQueue()**: Update queue metadata

---

## Testing Strategy

### Unit Tests
- QueueDaoTest (10 tests) - All should pass
- QueueRepositoryImplTest (13 tests) - All should pass
- Total: 23 tests must pass

### Scenarios to Test
1. Create queue → verify it's created with correct ID
2. Update queue → verify changes persist
3. Switch active queue → verify active status changes
4. Add episode → verify episode appears in queue
5. Remove episode → verify episode removed
6. Reorder episodes → verify new order persists
7. Clear queue → verify all episodes removed
8. Move episode → verify episode appears in target queue
9. Delete queue → verify queue no longer exists

### Regression Testing
- No new failures in existing database tests
- No changes to public API behavior
- Same events posted to EventBus
- Same error handling behavior

---

## Commit Strategy

Recommend breaking this into multiple commits (not one massive commit):

1. Commit 1: Add queue methods to DBWriter
2. Commit 2: Update QueueRepositoryImpl delegation (one method at a time or all at once)
3. Commit 3: Remove ExecutorService from QueueRepositoryImpl
4. Commit 4: Verify tests and document changes

---

## Risks and Mitigation

### Risk 1: Threading Issues
- **Problem**: Moving to shared executor could reveal hidden concurrency bugs
- **Mitigation**: Run all tests multiple times, test on real device, monitor for race conditions

### Risk 2: Event Posting Changes
- **Problem**: Events might post on different threads now
- **Mitigation**: Ensure EventBus handling is thread-safe (use ThreadMode annotations correctly)

### Risk 3: Exception Handling Differences
- **Problem**: Error handling might differ between custom executor and DBWriter
- **Mitigation**: Keep error handling consistent, test error scenarios

### Risk 4: Performance
- **Problem**: Single shared executor might be slower if queue operations frequently
- **Mitigation**: Profile if needed, but correctness > performance for database operations

---

## Success Criteria

✅ All criteria must be met:
- [ ] No `ExecutorService` in QueueRepositoryImpl
- [ ] All 9 queue operations delegate to DBWriter
- [ ] All 23 queue unit tests pass
- [ ] No compile errors or warnings
- [ ] No new checkstyle violations
- [ ] Same events posted to EventBus
- [ ] Code review passes

---

## Time Breakdown

| Task | Hours |
|------|-------|
| Analysis & understanding | 1-2 |
| Add methods to DBWriter | 3-4 |
| Update QueueRepositoryImpl delegation | 2-3 |
| Testing & bug fixing | 1-2 |
| Code review & refinement | 1 |
| **Total** | **8-10** |

---

## References

- DBWriter.java: `storage/database/src/main/java/de/danoeh/antennapod/storage/database/DBWriter.java`
- QueueRepositoryImpl.java: `storage/database/src/main/java/de/danoeh/antennapod/storage/database/QueueRepositoryImpl.java`
- QueueRepository.java: `model/src/main/java/de/danoeh/antennapod/model/feed/QueueRepository.java` (interface)
