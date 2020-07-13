package de.test.antennapod.entities;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SmallTest;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.util.playback.ExternalMedia;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link ExternalMedia} entity.
 */
@SmallTest
public class ExternalMediaTest {

    private static final int NOT_SET = -1;

    @After
    public void tearDown() throws Exception {
        clearSharedPrefs();
    }

    @SuppressLint("CommitPrefEdits")
    private void clearSharedPrefs() {
        SharedPreferences prefs = getDefaultSharedPrefs();
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.commit();
    }

    private SharedPreferences getDefaultSharedPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    @Test
    public void testSaveCurrentPositionUpdatesPreferences() {
        final int POSITION = 50;
        final int LAST_PLAYED_TIME = 1650;

        assertEquals(NOT_SET, getDefaultSharedPrefs().getInt(ExternalMedia.PREF_POSITION, NOT_SET));
        assertEquals(NOT_SET, getDefaultSharedPrefs().getLong(ExternalMedia.PREF_LAST_PLAYED_TIME, NOT_SET));

        ExternalMedia media = new ExternalMedia("source", MediaType.AUDIO);
        media.saveCurrentPosition(getDefaultSharedPrefs(), POSITION, LAST_PLAYED_TIME);

        assertEquals(POSITION, getDefaultSharedPrefs().getInt(ExternalMedia.PREF_POSITION, NOT_SET));
        assertEquals(LAST_PLAYED_TIME, getDefaultSharedPrefs().getLong(ExternalMedia.PREF_LAST_PLAYED_TIME, NOT_SET));
    }
}
