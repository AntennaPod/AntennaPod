package de.danoeh.antennapod.storage.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import de.danoeh.antennapod.model.feed.Queue;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

/**
 * Integration tests for database migration from old schema to new queues schema.
 *
 * <p>Tests verify that:
 * 1. Database schema is created correctly on fresh install
 * 2. Queues and QueueMembership tables exist with proper constraints
 * 3. Default queue is created automatically
 * 4. Foreign key constraints work correctly
 * 5. Unique constraints on queue names are enforced
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.TIRAMISU})
public class MigrationTest {

    private Context context;
    private PodDBAdapter adapter;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        PodDBAdapter.init(context);
        UserPreferences.init(context);
        adapter = PodDBAdapter.getInstance();
    }

    @After
    public void tearDown() {
        PodDBAdapter.tearDownTests();
    }

    /**
     * T015.1: Verify Queues table was created with correct schema
     */
    @Test
    public void testQueuesTableCreated() {
        Queue defaultQueue = adapter.selectDefaultQueue();
        assertNotNull("Queues table should exist and have default queue", defaultQueue);
        assertEquals("Default queue should be ID 1", 1, defaultQueue.getId());
    }

    /**
     * T015.2: Verify QueueMembership table exists
     */
    @Test
    public void testQueueMembershipTableCreated() {
        Queue queue = new Queue("MigrationTest", false, false);
        long queueId = adapter.insertQueue(queue);

        adapter.insertQueueMembership(queueId, 100, 0);
        int count = adapter.countQueueEpisodes(queueId);

        assertEquals("QueueMembership table should exist and allow inserts", 1, count);
    }

    /**
     * T015.3: Verify unique constraint on queue names (from schema migration)
     */
    @Test
    public void testQueueNameUniqueConstraint() {
        Queue queue1 = new Queue("Unique", false, false);
        adapter.insertQueue(queue1);

        Queue queue2 = new Queue("Unique", false, false);
        try {
            adapter.insertQueue(queue2);
            assertTrue("Should have thrown SQLiteConstraintException", false);
        } catch (android.database.sqlite.SQLiteConstraintException e) {
            assertTrue("Exception message should mention constraint",
                    e.getMessage().toLowerCase().contains("constraint"));
        }
    }

    /**
     * T015.4: Verify foreign key constraint is enabled (cascade delete)
     */
    @Test
    public void testForeignKeyConstraintEnabled() {
        Queue queue = new Queue("ForeignKeyTest", false, false);
        long queueId = adapter.insertQueue(queue);

        adapter.insertQueueMembership(queueId, 200, 0);
        adapter.insertQueueMembership(queueId, 201, 1);

        adapter.deleteQueue(queueId);

        int countAfterDelete = adapter.countQueueEpisodes(queueId);
        assertEquals("Cascade delete should remove memberships", 0, countAfterDelete);
    }

    /**
     * T015.5: Verify default queue initialization
     */
    @Test
    public void testDefaultQueueInitialization() {
        Queue defaultQueue = adapter.selectDefaultQueue();

        assertNotNull("Default queue must exist", defaultQueue);
        assertEquals("Default queue ID should be 1", 1, defaultQueue.getId());
        assertEquals("Default queue name should be 'Default'", "Default", defaultQueue.getName());
        assertTrue("Default queue should be marked as default", defaultQueue.isDefault());
        assertTrue("Default queue should be marked as active", defaultQueue.isActive());
    }

    /**
     * T015.6: Verify active queue is set on initialization
     */
    @Test
    public void testActiveQueueInitialization() {
        Queue activeQueue = adapter.selectActiveQueue();

        assertNotNull("Active queue must exist", activeQueue);
        assertTrue("Active queue should have isActive=true", activeQueue.isActive());
    }

    /**
     * T015.7: Verify multiple queues can be created after migration
     */
    @Test
    public void testMultipleQueuesAfterMigration() {
        Queue queue1 = new Queue("WorkoutQueue", false, false);
        Queue queue2 = new Queue("CommuteQueue", false, false);
        Queue queue3 = new Queue("EducationQueue", false, false);

        long id1 = adapter.insertQueue(queue1);
        long id2 = adapter.insertQueue(queue2);
        long id3 = adapter.insertQueue(queue3);

        assertTrue("All queue IDs should be positive", id1 > 0 && id2 > 0 && id3 > 0);

        List<Queue> allQueues = adapter.selectAllQueues();
        assertTrue("Should have at least 4 queues (default + 3 new)", allQueues.size() >= 4);
    }

    /**
     * T015.8: Verify queue metadata is preserved
     */
    @Test
    public void testQueueMetadataPersist() {
        Queue queue = new Queue("AttributeTest", false, false);
        long id = adapter.insertQueue(queue);

        Queue retrieved = adapter.selectQueueById(id);
        assertNotNull("Queue should be retrievable", retrieved);
        assertEquals("Name should be preserved", "AttributeTest", retrieved.getName());
    }

    /**
     * T015.9: Verify queue membership order is maintained
     */
    @Test
    public void testQueueMembershipOrdering() {
        Queue queue = new Queue("OrderTest", false, false);
        long queueId = adapter.insertQueue(queue);

        adapter.insertQueueMembership(queueId, 300, 0);
        adapter.insertQueueMembership(queueId, 301, 1);
        adapter.insertQueueMembership(queueId, 302, 2);

        int maxPosition = adapter.getMaxPositionInQueue(queueId);
        assertEquals("Max position should be 2", 2, maxPosition);
    }

    /**
     * T015.10: Verify database survives multiple queue operations
     */
    @Test
    public void testDatabaseStability() {
        // Create and delete multiple queues
        for (int i = 0; i < 5; i++) {
            Queue q = new Queue("TempQueue" + i, false, false);
            long id = adapter.insertQueue(q);
            adapter.deleteQueue(id);
        }

        // Default queue should still exist
        Queue defaultQueue = adapter.selectDefaultQueue();
        assertNotNull("Default queue should survive operations", defaultQueue);

        // Should be able to create new queues
        Queue finalQueue = new Queue("FinalQueue", false, false);
        long finalId = adapter.insertQueue(finalQueue);
        Queue retrieved = adapter.selectQueueById(finalId);
        assertNotNull("Should be able to create queue after operations", retrieved);
    }
}
