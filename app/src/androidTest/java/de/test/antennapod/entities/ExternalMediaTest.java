package de.test.antennapod.entities;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.InstrumentationTestCase;

import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.util.playback.ExternalMedia;

/**
 * Tests for {@link ExternalMedia} entity.
 */
public class ExternalMediaTest extends InstrumentationTestCase {

    private static final int NOT_SET = -1;

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
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
        return PreferenceManager.getDefaultSharedPreferences(getInstrumentation().getTargetContext());
    }

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
