package de.danoeh.antennapod.playback.service;

import android.content.Context;
import android.os.Looper;
import android.os.Bundle;
import com.google.common.util.concurrent.ListenableFuture;
import androidx.media3.common.Player;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.session.MediaSession;
import androidx.media3.session.SessionCommand;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.event.playback.SleepTimerUpdatedEvent;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueueStub;
import de.danoeh.antennapod.playback.base.MediaItemAdapter;
import de.danoeh.antennapod.storage.database.FeedDatabaseWriter;
import de.danoeh.antennapod.storage.database.PodDBAdapter;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.playback.service.internal.ClockSleepTimer;
import de.danoeh.antennapod.playback.service.internal.EpisodeSleepTimer;
import de.danoeh.antennapod.storage.preferences.SleepTimerPreferences;
import org.greenrobot.eventbus.EventBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowSystemClock;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(instrumentedPackages = "de.danoeh.antennapod.playback.service.internal")
public class SleepTimerInteractionTest {
    private static final long FULL_DURATION = TimeUnit.MINUTES.toMillis(35);
    private static final long REMAINING_DURATION = TimeUnit.MINUTES.toMillis(10);
    private Media3PlaybackService service;
    private ClockSleepTimer timer;
    private final MediaSession session = mock(MediaSession.class);
    private final MediaSession.ControllerInfo controller = mock(MediaSession.ControllerInfo.class);
    private final Player player = mock(Player.class);

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences(SleepTimerPreferences.PREF_NAME, Context.MODE_PRIVATE).edit().clear().commit();
        SleepTimerPreferences.init(context);
        UserPreferences.init(context);
        PlaybackPreferences.init(context);
        PodDBAdapter.init(context);
        PodDBAdapter.deleteDatabase();
        SynchronizationQueue.setInstance(new SynchronizationQueueStub());
        SleepTimerPreferences.setShakeToReset(false);
        service = Robolectric.buildService(Media3PlaybackService.class).get();
        when(session.getPlayer()).thenReturn(player);
        when(player.getPlayWhenReady()).thenReturn(true);
        timer = new TestClockSleepTimer(context);
        timer.start(FULL_DURATION);
        timer.updateRemainingTime(REMAINING_DURATION);
        ReflectionHelpers.setField(service, "sleepTimer", timer);
    }

    @After
    public void tearDown() {
        if (EventBus.getDefault().isRegistered(timer)) {
            timer.stop();
        }
        if (EventBus.getDefault().isRegistered(service)) {
            EventBus.getDefault().unregister(service);
        }
        EventBus.getDefault().removeAllStickyEvents();
        PodDBAdapter.tearDownTests();
    }

    @Test
    public void indexedEpisodeSelectionRestartsWithFullDurationWhenEnabled() {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        interact(Player.COMMAND_SEEK_TO_MEDIA_ITEM);
        assertEquals(FULL_DURATION, timer.getTimeLeft().getMillisValue());
        assertEquals(FULL_DURATION,
                EventBus.getDefault().getStickyEvent(SleepTimerUpdatedEvent.class).getMillisTimeLeft());
        ((TestClockSleepTimer) timer).advanceTime(1000);
        timer.playbackPositionUpdate(new PlaybackPositionEvent(1000, 3600000));
        assertEquals(FULL_DURATION - 1000, timer.getTimeLeft().getMillisValue());
    }

    @Test
    public void selectingANewEpisodeRestartsFullDuration() throws Exception {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        Feed feed = new Feed("url", null, null);
        feed.setItems(new ArrayList<>());
        FeedItem item = new FeedItem();
        item.setItemIdentifier("new-episode");
        item.setTitle("New episode");
        item.setMedia(new FeedMedia(item, "http://example.com", 2, "mime"));
        item.setFeed(feed);
        feed.getItems().add(item);
        FeedMedia media = FeedDatabaseWriter.updateFeed(RuntimeEnvironment.getApplication(), feed, false)
                .getItems().get(0).getMedia();
        MediaItem selected = MediaItemAdapter.fromMediaIdStub(media.getId());
        await(service.sessionCallback.onSetMediaItems(session, controller, Collections.singletonList(selected),
                C.INDEX_UNSET, C.TIME_UNSET));
        assertEquals(FULL_DURATION, timer.getTimeLeft().getMillisValue());
    }

    @Test
    public void unsuccessfulVoiceSearchPreservesRemainingTime() throws Exception {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        MediaItem request = MediaItem.EMPTY.buildUpon().setRequestMetadata(
                new MediaItem.RequestMetadata.Builder().setSearchQuery("No matching episode").build()).build();
        MediaSession.MediaItemsWithStartPosition result = await(service.sessionCallback.onSetMediaItems(
                session, controller, Collections.singletonList(request), C.INDEX_UNSET, C.TIME_UNSET));
        assertEquals(0, result.mediaItems.size());
        assertEquals(REMAINING_DURATION, timer.getTimeLeft().getMillisValue());
    }

    @Test
    public void missingEpisodePreservesRemainingTime() throws Exception {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        MediaItem request = MediaItemAdapter.fromMediaIdStub(Long.MAX_VALUE);
        MediaSession.MediaItemsWithStartPosition result = await(service.sessionCallback.onSetMediaItems(
                session, controller, Collections.singletonList(request), C.INDEX_UNSET, C.TIME_UNSET));
        assertEquals(0, result.mediaItems.size());
        assertEquals(REMAINING_DURATION, timer.getTimeLeft().getMillisValue());
    }

    @Test
    public void interactionDuringFadeRestoresVolume() {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        ReflectionHelpers.setField(service, "player", player);
        ReflectionHelpers.setField(service, "mediaSession", mock(Media3PlaybackService.MediaLibrarySession.class));
        EventBus.getDefault().register(service);
        timer.updateRemainingTime(5000);
        EventBus.getDefault().postSticky(SleepTimerUpdatedEvent.updated(timer.getTimeLeft()));
        verify(player).setVolume(0.5f);
        interact(Player.COMMAND_SEEK_TO_MEDIA_ITEM);
        verify(player, atLeastOnce()).setVolume(1.0f);
        assertEquals(FULL_DURATION, timer.getTimeLeft().getMillisValue());
    }

    @Test
    public void actionsPreserveRemainingTimeByDefault() {
        assertFalse(SleepTimerPreferences.resetOnPlaybackInteraction());
        interact(Player.COMMAND_PLAY_PAUSE);
        interact(Player.COMMAND_SEEK_TO_MEDIA_ITEM);
        interact(Player.COMMAND_SET_SPEED_AND_PITCH);
        assertEquals(REMAINING_DURATION, timer.getTimeLeft().getMillisValue());
    }

    @Test
    public void repeatedActionsEachRestartTheTimer() {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        interact(Player.COMMAND_SEEK_TO_MEDIA_ITEM);
        timer.updateRemainingTime(5000);
        interact(Player.COMMAND_SEEK_TO_MEDIA_ITEM);
        assertEquals(FULL_DURATION, timer.getTimeLeft().getMillisValue());
    }

    @Test
    public void otherSeekAndSpeedCommandsPreserveRemainingTime() throws Exception {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        int[] commands = {Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
                Player.COMMAND_SEEK_BACK, Player.COMMAND_SEEK_FORWARD, Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM, Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM, Player.COMMAND_SET_SPEED_AND_PITCH};
        for (int command : commands) {
            interact(command);
            assertEquals(REMAINING_DURATION, timer.getTimeLeft().getMillisValue());
        }
        for (String action : new String[] {"playback_speed", "next_chapter"}) {
            service.sessionCallback.onCustomCommand(session, controller, new SessionCommand(action, Bundle.EMPTY),
                    Bundle.EMPTY).get();
            assertEquals(REMAINING_DURATION, timer.getTimeLeft().getMillisValue());
        }
    }

    @Test
    public void pauseAndResumePreserveRemainingTime() {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        when(player.getPlayWhenReady()).thenReturn(false);
        interact(Player.COMMAND_PLAY_PAUSE);
        assertEquals(REMAINING_DURATION, timer.getTimeLeft().getMillisValue());
        when(player.getPlayWhenReady()).thenReturn(true);
        interact(Player.COMMAND_PLAY_PAUSE);
        assertEquals(REMAINING_DURATION, timer.getTimeLeft().getMillisValue());
    }

    @Test
    public void disabledTimerStaysDisabled() {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        timer.stop();
        interact(Player.COMMAND_SEEK_TO_MEDIA_ITEM);
        assertFalse(timer.isActive());
        assertEquals(0, timer.getTimeLeft().getMillisValue());
    }

    @Test
    public void expiredTimerIsNotReactivatedByEpisodeSelection() {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        timer.updateRemainingTime(0);
        interact(Player.COMMAND_SEEK_TO_MEDIA_ITEM);
        assertFalse(timer.isActive());
    }

    @Test
    public void episodeTimerIsNotRestarted() {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        timer.stop();
        timer = new EpisodeSleepTimer(RuntimeEnvironment.getApplication());
        timer.start(3);
        timer.updateRemainingTime(1);
        ReflectionHelpers.setField(service, "sleepTimer", timer);
        interact(Player.COMMAND_SEEK_TO_MEDIA_ITEM);
        assertEquals(1, timer.getTimeLeft().getDisplayValue());
    }

    @Test
    public void resetStartsANewWarningCycleAndResetsTickBaseline() {
        WarningTimer warningTimer = new WarningTimer(RuntimeEnvironment.getApplication());
        timer.stop();
        timer = warningTimer;
        timer.start(FULL_DURATION);
        SleepTimerPreferences.setVibrate(true);
        timer.updateRemainingTime(5000);
        ((TestClockSleepTimer) timer).advanceTime(1000);
        timer.playbackPositionUpdate(new PlaybackPositionEvent(1000, 3600000));
        assertEquals(1, warningTimer.vibrations);
        ((TestClockSleepTimer) timer).advanceTime(5000);
        timer.reset();
        timer.playbackPositionUpdate(new PlaybackPositionEvent(1000, 3600000));
        assertEquals(FULL_DURATION, timer.getTimeLeft().getMillisValue());
        timer.updateRemainingTime(5000);
        ((TestClockSleepTimer) timer).advanceTime(1000);
        timer.playbackPositionUpdate(new PlaybackPositionEvent(2000, 3600000));
        assertEquals(2, warningTimer.vibrations);
        assertTrue(timer.isActive());
    }

    private <T> T await(ListenableFuture<T> future) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!future.isDone() && System.nanoTime() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(10);
        }
        return future.get(0, TimeUnit.SECONDS);
    }

    private void interact(int command) {
        service.sessionCallback.onPlayerInteractionFinished(session, controller,
                new Player.Commands.Builder().add(command).build());
    }

    private static class TestClockSleepTimer extends ClockSleepTimer {
        TestClockSleepTimer(Context context) {
            super(context);
        }

        void advanceTime(long millis) {
            ShadowSystemClock.advanceBy(millis, TimeUnit.MILLISECONDS);
        }
    }

    private static class WarningTimer extends TestClockSleepTimer {
        int vibrations;

        WarningTimer(Context context) {
            super(context);
        }

        @Override
        protected void vibrate() {
            vibrations++;
        }
    }
}
