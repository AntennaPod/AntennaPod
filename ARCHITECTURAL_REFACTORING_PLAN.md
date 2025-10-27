# Architectural Refactoring Plan - Multiple Named Queues Feature

**Priority**: CRITICAL - Must be completed before Phase 5
**Scope**: Fix foundational architecture issues identified in PR #8066 review
**Reviewer Feedback**: ByteHamster (AntennaPod Maintainer)

---

## Executive Summary

The Multiple Named Queues feature (Phases 1-3) introduced architectural deviations from AntennaPod's established patterns. These issues must be fixed before continuing to Phase 5, as they would otherwise propagate further into the codebase.

**Critical Issues**: 5
**High Priority Issues**: 2
**Estimated Effort**: 15-20 hours

---

## Issue 1: Custom ExecutorService (CRITICAL)

### Problem
QueueRepositoryImpl creates its own ExecutorService (`dbExec`) instead of using AntennaPod's established DBWriter pattern.

**File**: `storage/database/src/main/java/de/danoeh/antennapod/storage/database/QueueRepositoryImpl.java`
**Lines**: 43-50 (ExecutorService creation)

### Impact
- ❌ Creates parallel thread pool, violating single-threaded database access principle
- ❌ Queue operations run on different thread than other DB operations
- ❌ Risk of race conditions and synchronization issues
- ❌ Breaks AntennaPod's established DBWriter pattern
- ❌ Maintainers noted: "We can't use a different thread here"

### Solution
**Integrate with DBWriter pattern**:

1. **Remove QueueRepositoryImpl's ExecutorService** (lines 43-50)
   - Delete `dbExec` field and static initialization
   - Remove `dbExec.submit()` calls throughout

2. **Add queue methods to DBWriter.java** instead:
   ```java
   // In DBWriter.java (existing data access class)
   public static Future<?> switchActiveQueue(long queueId) {
       return dbExec.submit(() -> {
           // Switch logic using existing dbExec
       });
   }
   ```

3. **Have QueueRepositoryImpl delegate to DBWriter**:
   ```java
   @Override
   public Future<?> switchActiveQueue(long queueId) {
       return DBWriter.switchActiveQueue(queueId);
   }
   ```

### Benefits
- ✅ Uses AntennaPod's single database thread
- ✅ Consistent with all other DB operations
- ✅ Eliminates threading complexity
- ✅ Follows established pattern
- ✅ Safer for concurrent access

### Affected Methods
- createQueue()
- deleteQueue()
- updateQueue()
- switchActiveQueue()
- addEpisodeToQueue()
- removeEpisodeFromQueue()
- reorderQueueEpisodes()
- clearQueue()
- moveEpisodeBetweenQueues()

### Timeline
- **Analysis**: 2 hours
- **Implementation**: 4-5 hours
- **Testing**: 2-3 hours
- **Total**: 8-10 hours

---

## Issue 2: Queue Model Design (CRITICAL)

### Problem
Queue model stores `isDefault` and `isActive` as mutable attributes. This creates synchronization risk - these values must be globally unique (only one default queue, only one active queue), but are stored in individual objects.

**File**: `model/src/main/java/de/danoeh/antennapod/model/feed/Queue.java`
**Lines**: 27-28 (problem fields), 141-169 (setter methods)

### Impact
- ❌ Risk of multiple queues becoming "default" or "active" simultaneously
- ❌ Objects can get out of sync with database state
- ❌ Code has to manually enforce uniqueness constraints
- ❌ Maintainer noted: "I would not model this as attributes of the queue but outside of it. Then nothing can go out of sync"

### Solution
**Move isDefault and isActive to external state management**:

#### Option A: Minimal Change (Recommended)
1. **Keep fields in Queue model** but mark as database-only
2. **Move write logic to repository**:
   - Repository enforces unique constraints
   - When setting a queue as default/active, repository updates DB and invalidates other queues' cached state

3. **Add read-only methods**:
   ```java
   public boolean isDefault() { return isDefault; } // Read from DB
   public boolean isActive() { return isActive; }   // Read from DB
   ```

4. **Remove setters from Queue class**:
   - Delete setDefault() and setActive() methods
   - Only repository can change these via DB operations

#### Option B: Complete Refactoring
1. **Remove isDefault and isActive from Queue model entirely**
2. **Add to QueueState external class**:
   ```java
   public class QueueState {
       long defaultQueueId;
       long activeQueueId;
   }
   ```

3. **Repository manages QueueState separately**
4. **UI gets state from repository, not from Queue object**

#### Recommendation
Start with **Option A** (minimal change):
- Less refactoring required
- DB schema unchanged
- Fixes the core issue (read-only attributes)
- Can evolve to Option B later if needed

### Changes Required

**Queue.java**:
- Remove `setDefault()` method (line 151-154)
- Remove `setActive()` method (line 166-169)
- Add comments: "These attributes are read-only, managed by QueueRepository"

**QueueRepositoryImpl.java**:
- Update `switchActiveQueue()` to handle state changes properly
- Update `createQueue()` to not call setDefault()
- Add proper database update logic

**QueueViewModel.java**:
- No changes needed (already treats queues as read-only from UI perspective)

### Affected Code
- QueueRepository.createQueue() - creates default queue
- QueueRepositoryImpl.switchActiveQueue() - manages active state
- QueueViewModel - doesn't need changes

### Timeline
- **Analysis**: 2 hours
- **Implementation (Option A)**: 3-4 hours
- **Testing**: 2 hours
- **Total**: 7-8 hours

---

## Issue 3: ViewBinding in UI Components (HIGH)

### Problem
UI components use `findViewById()` instead of AndroidX ViewBinding, which is AntennaPod's established pattern.

**Files**:
- `ui/common/src/main/java/de/danoeh/antennapod/ui/common/QueueColorPicker.java`
- `ui/common/src/main/java/de/danoeh/antennapod/ui/common/QueueIconPicker.java`
- `ui/common/src/main/java/de/danoeh/antennapod/ui/common/QueueSwitchBottomSheet.java`

### Impact
- ❌ Doesn't follow AntennaPod's established ViewBinding pattern
- ❌ More verbose and error-prone than ViewBinding
- ❌ Maintainer noted: "Use ViewBinding please"
- ⚠️ Not a functionality issue, but consistency/maintainability issue

### Solution

1. **Generate binding classes** (automatically from layouts):
   - Android Gradle Plugin generates `QueueColorPickerBinding`, `QueueIconPickerBinding`, etc.

2. **Replace findViewById calls**:
   ```java
   // OLD
   ColorPicker colorPicker = findViewById(R.id.colorPicker);

   // NEW
   binding = QueueColorPickerBinding.bind(view);
   binding.colorPicker.setOnColorSelectedListener(...);
   ```

3. **Use binding object throughout component**

### Affected Files
- `ui/common/src/main/java/de/danoeh/antennapod/ui/common/QueueColorPicker.java`
- `ui/common/src/main/java/de/danoeh/antennapod/ui/common/QueueIconPicker.java`
- `ui/common/src/main/java/de/danoeh/antennapod/ui/common/QueueSwitchBottomSheet.java`

### Timeline
- **Analysis**: 1 hour
- **Implementation**: 2-3 hours (3 files)
- **Testing**: 1 hour
- **Total**: 4-5 hours

---

## Issue 4: Exception Hierarchy (MEDIUM)

### Problem
Custom exceptions may be premature/unnecessary:
- `DefaultQueueException` - For preventing deletion of default queue
- `QueueNameExistsException` - For duplicate name validation
- `QueueNotFoundException` - Might be handled as null return instead

**Files**:
- `model/src/main/java/de/danoeh/antennapod/model/feed/DefaultQueueException.java`
- `model/src/main/java/de/danoeh/antennapod/model/feed/QueueNameExistsException.java`
- `model/src/main/java/de/danoeh/antennapod/model/feed/QueueNotFoundException.java`

### Maintainer Feedback
- "I don't understand why we need an exception for that. The button should not be shown to users in the first place for the default queue."
- "I don't think there needs to be an exception for that"

### Analysis

#### DefaultQueueException
- **Purpose**: Prevent deletion of default queue
- **Better approach**: Hide delete button for default queue in UI (business logic, not exception)
- **Action**: Remove exception, handle in ViewModel instead

#### QueueNameExistsException
- **Purpose**: Validate queue name uniqueness
- **Better approach**: Validate before showing create dialog, handle errors as messages not exceptions
- **Action**: Consider removing or simplifying

#### QueueNotFoundException
- **Purpose**: Signal queue doesn't exist
- **Assessment**: Reasonable to keep (distinguishes from other errors)
- **Action**: Keep, but review usage

### Solution
1. **Remove DefaultQueueException**:
   - UI shouldn't show delete option for default queue
   - ViewModel can validate without throwing exception

2. **Review QueueNameExistsException**:
   - Consider if validation error message is sufficient
   - Or keep as optional exception for advanced scenarios

3. **Keep QueueNotFoundException** (reasonable exception)

### Affected Code
- QueueViewModel.deleteQueue()
- QueueViewModel.createQueue()
- Error handling in UI

### Timeline
- **Analysis**: 2 hours
- **Implementation**: 2-3 hours
- **Testing**: 1 hour
- **Total**: 5-6 hours

---

## Issue 5: Event Pattern Alignment (MEDIUM)

### Problem
Introduced `QueueContentChangedEvent` but AntennaPod already has `QueueEvent` with action types. Creates parallel event systems.

**Files**:
- `event/src/main/java/de/danoeh/antennapod/event/QueueContentChangedEvent.java` (new)
- `event/src/main/java/de/danoeh/antennapod/event/QueueEvent.java` (existing)

### Impact
- ❌ Two parallel event systems for queue changes
- ❌ Components subscribe to both for complete picture
- ❌ Confusing for maintainers
- ❌ Maintainer noted: "We already have an event for that"

### Solution
**Consolidate to use QueueEvent**:

1. **Remove QueueContentChangedEvent.java**
2. **Use QueueEvent with appropriate action types**:
   ```java
   // Old approach
   EventBus.post(new QueueContentChangedEvent(queueId));

   // New approach
   QueueEvent event = new QueueEvent();
   event.action = QueueEvent.Action.SWITCHED; // or other action
   EventBus.post(event);
   ```

3. **Update QueueRepositoryImpl** to post QueueEvent instead

### Affected Code
- QueueRepositoryImpl - Replace QueueContentChangedEvent posts with QueueEvent
- Any listeners for QueueContentChangedEvent - Switch to QueueEvent

### Timeline
- **Analysis**: 1 hour
- **Implementation**: 2 hours
- **Testing**: 1 hour
- **Total**: 4 hours

---

## Implementation Priority & Sequencing

### Must Fix First (Blocking)
1. **Issue 1: ExecutorService** (8-10 hours)
   - Blocks: Everything else (threading issue affects all operations)
   - Do first: Fundamental to architecture

2. **Issue 2: Queue Model** (7-8 hours)
   - Blocks: Phase 5 implementation
   - Do second: Model changes affect UI logic

### Should Fix (High Priority)
3. **Issue 3: ViewBinding** (4-5 hours)
   - Blocks: Code review acceptance
   - Do third: Consistency issue, UI work

4. **Issue 5: Event Alignment** (4 hours)
   - Blocks: Clean event handling
   - Do fourth: Communication pattern

### Nice to Fix (Medium Priority)
5. **Issue 4: Exceptions** (5-6 hours)
   - Blocks: Nothing critical
   - Do last: Design cleanup

---

## Testing Strategy

### Unit Tests
- QueueRepositoryImplTest - All 13 tests should still pass
- QueueDaoTest - All 10 tests should still pass
- Add new tests for Queue read-only state

### Integration Tests
- Espresso tests (T036-T037) should work unchanged
- Test queue switching behavior
- Test default queue protection

### Manual Testing
- Create, switch, delete queues
- Verify no sync issues with default/active states
- Verify ViewBinding updates UI correctly

---

## Risk Assessment

### High Risk
- ✅ ExecutorService refactoring (complex threading logic, must be correct)
- ✅ Queue model changes (affects entire feature)

### Medium Risk
- ⚠️ ViewBinding changes (straightforward but affects UI)
- ⚠️ Event consolidation (must verify listeners still work)

### Low Risk
- ✅ Exception cleanup (mostly additive, removing unused exceptions)

### Mitigation
- Run full test suite after each major change
- Test on actual device/emulator
- Get code review after each issue is fixed

---

## Success Criteria

- ✅ QueueRepositoryImpl uses DBWriter's ExecutorService
- ✅ Queue isDefault/isActive are read-only from outside repository
- ✅ All UI components use ViewBinding
- ✅ Only QueueEvent is used for queue-related events
- ✅ Unnecessary exceptions removed
- ✅ All unit tests pass (33/33 queue tests)
- ✅ No regressions to existing functionality
- ✅ Code review passes

---

## Timeline

| Phase | Hours | Tasks | Priority |
|-------|-------|-------|----------|
| Issue 1 | 8-10 | DBWriter integration | CRITICAL |
| Issue 2 | 7-8 | Queue model refactoring | CRITICAL |
| Issue 3 | 4-5 | ViewBinding integration | HIGH |
| Issue 5 | 4 | Event consolidation | HIGH |
| Issue 4 | 5-6 | Exception cleanup | MEDIUM |
| **Total** | **30-35** | | |

---

## Next Steps

1. **Create detailed task list** for Issue 1 (DBWriter integration)
2. **Implement Issue 1** (foundational)
3. **Verify tests still pass**
4. **Implement Issue 2** (model refactoring)
5. **Continue with Issues 3-5**
6. **Create PR with all fixes**
7. **Request review from ByteHamster**

**Estimated Completion**: 3-4 weeks at 20 hours/week

---

## Notes

This refactoring is **not optional** - the architectural issues will cause problems if left unfixed:
- Threading issues will cause subtle bugs
- Model design will cause state synchronization problems
- ViewBinding inconsistency will cause maintenance issues
- Event duplication will cause confusion

**Better to fix now than discover issues in Phase 5 or production.**
