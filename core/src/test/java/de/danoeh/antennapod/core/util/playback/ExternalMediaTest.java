package de.danoeh.antennapod.core.util.playback;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import androidx.test.platform.app.InstrumentationRegistry;
import de.danoeh.antennapod.core.feed.MediaType;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link ExternalMedia} entity.
 */
@RunWith(RobolectricTestRunner.class)
public class ExternalMediaTest {

    private static final int NOT_SET = -1;
    private static final int POSITION = 50;
    private static final int LAST_PLAYED_TIME = 1650;

    @After
    public void tearDown() {
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
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Test
    public void testSaveCurrentPositionUpdatesPreferences() {
        assertEquals(NOT_SET, getDefaultSharedPrefs().getInt(ExternalMedia.PREF_POSITION, NOT_SET));
        assertEquals(NOT_SET, getDefaultSharedPrefs().getLong(ExternalMedia.PREF_LAST_PLAYED_TIME, NOT_SET));

        ExternalMedia media = new ExternalMedia("source", MediaType.AUDIO);
        media.saveCurrentPosition(getDefaultSharedPrefs(), POSITION, LAST_PLAYED_TIME);

        assertEquals(POSITION, getDefaultSharedPrefs().getInt(ExternalMedia.PREF_POSITION, NOT_SET));
        assertEquals(LAST_PLAYED_TIME, getDefaultSharedPrefs().getLong(ExternalMedia.PREF_LAST_PLAYED_TIME, NOT_SET));
    }
}
