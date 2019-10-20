package de.test.antennapod.playback;

import androidx.test.filters.LargeTest;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import org.junit.Before;

/**
 * Test cases for starting and ending playback from the MainActivity and AudioPlayerActivity.
 */
@LargeTest
public class PlaybackExoplayerTest extends PlaybackTest {
    @Before
    public void setUp() throws Exception {
        super.setUp();
        UserPreferences.enableExoplayer();
    }
}
