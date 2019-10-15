package de.test.antennapod.storage;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.danoeh.antennapod.core.storage.PodDBAdapter;

public class DownloadItemSelectorSerialImplTest {

    private static final String TAG = "DlItemSlctrSerialTest";

    @After
    public void tearDown() throws Exception {
        // Leave DB as-is so that if a test fails, the state of the DB can still be inspected.
        // setUp() will delete the database anyway.
        /// assertTrue(PodDBAdapter.deleteDatabase());
    }

    @Before
    public void setUp() throws Exception {
        // create new database
        PodDBAdapter.init(ApplicationProvider.getApplicationContext());
        PodDBAdapter.deleteDatabase();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.close();

    }

    @Test
    public void testGetAutoDownloadableEpisodes() throws Exception {
        // TODO-1077: test DownloadItemSelectorSerialImpl
        // - the picking of the feeditems are done in round-robin fashion
        //
        // - Also need to separately test the new DBAccess.getEpisodicToSerialRatio() method
        //   - it excludes non-autodl (and not updated) ones in calculating the ratio
    }
}
