package de.danoeh.antennapod.storage.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
 * Unit tests for Queue DAO operations using Robolectric.
 *
 * Tests the low-level database access for queue and queue membership operations.
 * Verifies CRUD operations, constraints (unique names, foreign keys), and cascade deletes.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.TIRAMISU})
public class QueueDaoTest {

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
     * T013.1: Verify default queue is created on database initialization
     */
    @Test
    public void testDefaultQueueExists() {
        Queue defaultQueue = adapter.selectDefaultQueue();
        assertNotNull("Default queue should exist", defaultQueue);
        assertEquals("Default queue ID should be 1", 1, defaultQueue.getId());
        assertEquals("Default queue name should be 'Default'", "Default", defaultQueue.getName());
        assertTrue("Default queue should have isDefault=true", defaultQueue.isDefault());
    }

    /**
     * T013.2: Verify active queue is set on initialization
     */
    @Test
    public void testActiveQueueExists() {
        Queue active = adapter.selectActiveQueue();
        assertNotNull("Active queue should exist", active);
        assertTrue("Active queue should have isActive=true", active.isActive());
    }

    /**
     * T013.3: Create queue and verify it's stored
     */
    @Test
    public void testCreateQueue() {
        Queue queue = new Queue("Test Queue", 0xFF1976D2, "ic_queue_music_24dp", false, false);
        long id = adapter.insertQueue(queue);
        assertTrue("Queue ID should be positive", id > 0);

        Queue retrieved = adapter.selectQueueById(id);
        assertNotNull("Queue should be retrievable", retrieved);
        assertEquals("Name should match", "Test Queue", retrieved.getName());
        assertEquals("Color should match", 0xFF1976D2, retrieved.getColor());
        assertEquals("Icon should match", "ic_queue_music_24dp", retrieved.getIcon());
    }

    /**
     * T013.4: Verify unique constraint on queue names
     */
    @Test
    public void testQueueNameUniqueness() {
        Queue queue1 = new Queue("UniqueTest", 0xFF1976D2, "ic_queue_music_24dp", false, false);
        adapter.insertQueue(queue1);

        Queue queue2 = new Queue("UniqueTest", 0xFF388E3C, "ic_directions_run_24dp", false, false);
        try {
            adapter.insertQueue(queue2);
            fail("Should have thrown exception for duplicate name");
        } catch (Exception e) {
            assertTrue("Should be SQLiteConstraintException",
                e instanceof android.database.sqlite.SQLiteConstraintException);
        }
    }

    /**
     * T013.5: Update queue and verify changes persist
     */
    @Test
    public void testUpdateQueue() {
        Queue queue = new Queue("Original", 0xFF1976D2, "ic_queue_music_24dp", false, false);
        long id = adapter.insertQueue(queue);

        queue.setId(id);
        queue.setName("Updated");
        queue.setColor(0xFF388E3C);
        adapter.updateQueue(queue);

        Queue retrieved = adapter.selectQueueById(id);
        assertEquals("Name should be updated", "Updated", retrieved.getName());
        assertEquals("Color should be updated", 0xFF388E3C, retrieved.getColor());
    }

    /**
     * T013.6: Delete queue and verify it's removed
     */
    @Test
    public void testDeleteQueue() {
        Queue queue = new Queue("ToDelete", 0xFF1976D2, "ic_queue_music_24dp", false, false);
        long id = adapter.insertQueue(queue);

        adapter.deleteQueue(id);

        Queue retrieved = adapter.selectQueueById(id);
        assertNull("Deleted queue should not exist", retrieved);
    }

    /**
     * T013.7: Add episode to queue via membership
     */
    @Test
    public void testAddEpisodeToQueue() {
        Queue queue = new Queue("Episodes", 0xFF1976D2, "ic_queue_music_24dp", false, false);
        long queueId = adapter.insertQueue(queue);

        adapter.insertQueueMembership(queueId, 100, 0);
        int count = adapter.countQueueEpisodes(queueId);
        assertEquals("Queue should have 1 episode", 1, count);
    }

    /**
     * T013.8: Remove episode from queue
     */
    @Test
    public void testRemoveEpisodeFromQueue() {
        Queue queue = new Queue("Removal", 0xFF1976D2, "ic_queue_music_24dp", false, false);
        long queueId = adapter.insertQueue(queue);

        adapter.insertQueueMembership(queueId, 100, 0);
        adapter.deleteQueueMembership(queueId, 100);

        int count = adapter.countQueueEpisodes(queueId);
        assertEquals("Queue should have 0 episodes after removal", 0, count);
    }

    /**
     * T013.9: Verify cascade delete removes memberships
     */
    @Test
    public void testCascadeDeleteMemberships() {
        Queue queue = new Queue("Cascade", 0xFF1976D2, "ic_queue_music_24dp", false, false);
        long queueId = adapter.insertQueue(queue);

        adapter.insertQueueMembership(queueId, 100, 0);
        adapter.insertQueueMembership(queueId, 101, 1);

        adapter.deleteQueue(queueId);

        int count = adapter.countQueueEpisodes(queueId);
        assertEquals("All memberships should be deleted via cascade", 0, count);
    }

    /**
     * T013.10: Get all queues ordered by active and name
     */
    @Test
    public void testGetAllQueues() {
        List<Queue> queues = adapter.selectAllQueues();
        assertNotNull("Queue list should not be null", queues);
        assertTrue("Should have at least default queue", queues.size() >= 1);

        // Verify first queue is active (active queues come first in ordering)
        assertTrue("First queue should be active", queues.get(0).isActive());
    }
}
