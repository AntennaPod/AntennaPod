package de.danoeh.antennapod.playback.service;

import android.content.Context;
import android.content.Intent;
import de.danoeh.antennapod.model.playback.MediaType;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.appstartintent.VideoPlayerActivityStarter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class PlayerActivityIntentTest {
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        PlaybackPreferences.init(context);
        PlaybackService.isRunning = false;
    }

    @After
    public void tearDown() {
        PlaybackPreferences.writeNoMediaPlaying();
        PlaybackService.isRunning = false;
    }

    @Test
    public void videoOpensMainPlayer() {
        assertMainPlayerIntent(PlaybackService.getPlayerActivityIntent(context, media(MediaType.VIDEO)));
    }

    @Test
    public void audioOpensMainPlayer() {
        assertMainPlayerIntent(PlaybackService.getPlayerActivityIntent(context, media(MediaType.AUDIO)));
    }

    @Test
    public void lastPlayedVideoOpensMainPlayer() {
        PlaybackPreferences.writeMediaPlaying(media(MediaType.VIDEO));
        assertMainPlayerIntent(PlaybackService.getPlayerActivityIntent(context));
    }

    @Test
    public void runningPlaybackOpensMainPlayer() {
        PlaybackService.isRunning = true;
        PlaybackPreferences.writeMediaPlaying(media(MediaType.VIDEO));
        assertMainPlayerIntent(PlaybackService.getPlayerActivityIntent(context));
    }

    @Test
    public void fullScreenVideoStaysInPlayerTask() {
        Intent intent = new VideoPlayerActivityStarter(context).getIntent();
        assertEquals(VideoPlayerActivityStarter.INTENT_MEDIA3, intent.getAction());
        assertEquals(context.getPackageName(), intent.getPackage());
        assertEquals(0, intent.getFlags() & (Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private Playable media(MediaType mediaType) {
        Playable media = mock(Playable.class);
        when(media.getMediaType()).thenReturn(mediaType);
        return media;
    }

    private void assertMainPlayerIntent(Intent intent) {
        assertEquals(MainActivityStarter.INTENT, intent.getAction());
        assertEquals(context.getPackageName(), intent.getPackage());
        assertTrue(intent.getBooleanExtra(MainActivityStarter.EXTRA_OPEN_PLAYER, false));
    }
}
