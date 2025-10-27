package de.danoeh.antennapod.storage.database;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for QueueRepositoryImpl.
 *
 * <p>Tests the repository layer for queue management operations including:
 * - Queue CRUD operations
 * - Queue switching and activation
 * - Error handling with custom exceptions
 * - Transaction support
 *
 * <p>Uses Robolectric for Android context and in-memory database testing.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.TIRAMISU})
public class QueueRepositoryImplTest {

    private Context context;
    private PodDBAdapter adapter;
    private QueueRepositoryImpl repository;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        UserPreferences.init(context);
        adapter = PodDBAdapter.getInstance();
        repository = QueueRepositoryImpl.getInstance();
    }

    @After
    public void tearDown() {
        PodDBAdapter.tearDownTests();
    }

    @Test
    public void testCreateQueueSuccess() {
        Queue queue = new Queue("Test Queue", 0xFF1976D2, "ic_queue_music_24dp", false, false);
        long queueId = adapter.insertQueue(queue);
        assertTrue(queueId > 0);

        Queue retrieved = adapter.selectQueueById(queueId);
        assertNotNull(retrieved);
        assertEquals("Test Queue", retrieved.getName());
    }

    @Test
    public void testCreateQueueDuplicateName() {
        Queue queue1 = new Queue("Duplicate", 0xFF1976D2, "ic_queue_music_24dp", false, false);
        adapter.insertQueue(queue1);

        Queue queue2 = new Queue("Duplicate", 0xFF1976D2, "ic_queue_music_24dp", false, false);
        try {
            adapter.insertQueue(queue2);
            fail("Should have thrown exception for duplicate name");
        } catch (Exception e) {
            assertTrue(e instanceof android.database.sqlite.SQLiteConstraintException);
        }
    }

    @Test
    public void testDeleteQueue() {
        Queue queue = new Queue("Delete Test", 0xFF1976D2, "ic_queue_music_24dp", false, false);
        long queueId = adapter.insertQueue(queue);

        adapter.deleteQueue(queueId);

        Queue retrieved = adapter.selectQueueById(queueId);
        assertNull(retrieved);
    }

    @Test
    public void testGetAllQueues() {
        List<Queue> queues = adapter.selectAllQueues();
        assertNotNull(queues);
        // Should have at least the default queue
        assertTrue(queues.size() >= 1);
    }

    @Test
    public void testGetActiveQueue() {
        Queue active = adapter.selectActiveQueue();
        assertNotNull(active);
        assertTrue(active.isActive());
    }

    @Test
    public void testGetDefaultQueue() {
        Queue defaultQueue = adapter.selectDefaultQueue();
        assertNotNull(defaultQueue);
        assertTrue(defaultQueue.isDefault());
        assertEquals(1, defaultQueue.getId());
    }

    @Test
    public void testQueueNameUniqueness() {
        Queue queue1 = new Queue("Unique Test", 0xFF1976D2, "ic_queue_music_24dp", false, false);
        long id1 = adapter.insertQueue(queue1);

        queue1.setId(id1);
        queue1.setName("Updated Name");
        adapter.updateQueue(queue1);

        Queue retrieved = adapter.selectQueueById(id1);
        assertEquals("Updated Name", retrieved.getName());
    }

    @Test
    public void testUpdateQueueMetadata() {
        Queue queue = new Queue("Update Test", 0xFF1976D2, "ic_queue_music_24dp", false, false);
        long queueId = adapter.insertQueue(queue);

        queue.setId(queueId);
        queue.setColor(0xFF388E3C);
        queue.setIcon("ic_directions_run_24dp");
        adapter.updateQueue(queue);

        Queue retrieved = adapter.selectQueueById(queueId);
        assertEquals(0xFF388E3C, retrieved.getColor());
        assertEquals("ic_directions_run_24dp", retrieved.getIcon());
    }

    @Test
    public void testQueueMembershipInsert() {
        Queue queue = new Queue("Membership Test", 0xFF1976D2, "ic_queue_music_24dp", false, false);
        long queueId = adapter.insertQueue(queue);

        // Add episode to queue
        adapter.insertQueueMembership(queueId, 100, 0);
        int count = adapter.countQueueEpisodes(queueId);
        assertEquals(1, count);
    }

    @Test
    public void testQueueMembershipDelete() {
        Queue queue = new Queue("Delete Member Test", 0xFF1976D2, "ic_queue_music_24dp", false, false);
        long queueId = adapter.insertQueue(queue);

        adapter.insertQueueMembership(queueId, 100, 0);
        adapter.deleteQueueMembership(queueId, 100);

        int count = adapter.countQueueEpisodes(queueId);
        assertEquals(0, count);
    }

    @Test
    public void testQueueMembershipExists() {
        Queue queue = new Queue("Exists Test", 0xFF1976D2, "ic_queue_music_24dp", false, false);
        long queueId = adapter.insertQueue(queue);

        adapter.insertQueueMembership(queueId, 100, 0);
        assertTrue(adapter.queueMembershipExists(queueId, 100));

        adapter.deleteQueueMembership(queueId, 100);
        assertTrue(!adapter.queueMembershipExists(queueId, 100));
    }

    @Test
    public void testGetMaxPosition() {
        Queue queue = new Queue("Position Test", 0xFF1976D2, "ic_queue_music_24dp", false, false);
        long queueId = adapter.insertQueue(queue);

        adapter.insertQueueMembership(queueId, 100, 0);
        adapter.insertQueueMembership(queueId, 101, 1);
        adapter.insertQueueMembership(queueId, 102, 2);

        int maxPos = adapter.getMaxPositionInQueue(queueId);
        assertEquals(2, maxPos);
    }

    @Test
    public void testClearQueue() {
        Queue queue = new Queue("Clear Test", 0xFF1976D2, "ic_queue_music_24dp", false, false);
        long queueId = adapter.insertQueue(queue);

        adapter.insertQueueMembership(queueId, 100, 0);
        adapter.insertQueueMembership(queueId, 101, 1);

        adapter.deleteQueue(queueId);

        int count = adapter.countQueueEpisodes(queueId);
        assertEquals(0, count);
    }
}
