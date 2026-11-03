package de.danoeh.antennapod.playback.service;

import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Looper;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaLibraryService.MediaLibrarySession;
import androidx.media3.session.MediaSession;
import androidx.media3.session.SessionCommand;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.event.playback.SleepTimerUpdatedEvent;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.download.serviceinterface.AutoDownloadManager;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueueStub;
import de.danoeh.antennapod.playback.base.MediaItemAdapter;
import de.danoeh.antennapod.playback.service.internal.ClockSleepTimer;
import de.danoeh.antennapod.playback.service.internal.EpisodeSleepTimer;
import de.danoeh.antennapod.playback.service.internal.MediaLibrarySessionCallback;
import de.danoeh.antennapod.playback.service.internal.ShakeListener;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.database.FeedDatabaseWriter;
import de.danoeh.antennapod.storage.database.PodDBAdapter;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.storage.preferences.SleepTimerPreferences;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(instrumentedPackages = "de.danoeh.antennapod.playback.service.internal")
public class SleepTimerInteractionTest {
    private static final long FULL_DURATION = TimeUnit.MINUTES.toMillis(35);
    private static final long REMAINING_DURATION = TimeUnit.MINUTES.toMillis(10);
    private Media3PlaybackService service;
    private ClockSleepTimer timer;
    private Player.Listener listener;
    private FeedMedia episode;
    private final MediaLibrarySession session = mock(MediaLibrarySession.class);
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
        when(player.getPlaybackParameters()).thenReturn(PlaybackParameters.DEFAULT);
        when(player.getPlaybackState()).thenReturn(Player.STATE_READY);
        ReflectionHelpers.setField(service, "player", player);
        ReflectionHelpers.setField(service, "mediaSession", session);
        listener = ReflectionHelpers.getField(service, "playerListener");
        EventBus.getDefault().register(service);
        timer = new TestClockSleepTimer(context);
        timer.start(FULL_DURATION);
        timer.updateRemainingTime(REMAINING_DURATION);
        ReflectionHelpers.setField(service, "sleepTimer", timer);
        episode = seedEpisode();
    }

    @After
    public void tearDown() {
        Disposable observer = ReflectionHelpers.getField(service, "positionObserverDisposable");
        if (observer != null) {
            observer.dispose();
        }
        if (EventBus.getDefault().isRegistered(timer)) {
            timer.stop();
        }
        EventBus.getDefault().unregister(service);
        EventBus.getDefault().removeAllStickyEvents();
        PodDBAdapter.tearDownTests();
    }

    @Test
    public void manualSelectionResetsImmediatelyWithoutWaitingForPlayback() throws Exception {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        setCurrent(Long.MAX_VALUE);
        SettableFuture<List<MediaItem>> loaded = SettableFuture.create();
        MediaLibrarySessionCallback delayed = spy(service.sessionCallback);
        doReturn(loaded).when(delayed).onAddMediaItems(any(), any(), any());
        ListenableFuture<MediaSession.MediaItemsWithStartPosition> selection = delayed.onSetMediaItems(
                session, controller, Collections.singletonList(MediaItemAdapter.fromMediaIdStub(episode.getId())),
                C.INDEX_UNSET, C.TIME_UNSET);
        assertFull();
        assertFalse(player.getPlayWhenReady());
        timer.updateRemainingTime(REMAINING_DURATION);
        loaded.setException(new IllegalStateException("Selection failed"));
        assertTrue(await(selection).mediaItems.isEmpty());
        listener.onPlayerError(new PlaybackException("Failure", null, PlaybackException.ERROR_CODE_UNSPECIFIED));
        playRequest(Player.COMMAND_SET_MEDIA_ITEM, Player.COMMAND_PREPARE, Player.COMMAND_PLAY_PAUSE);
        assertRemaining();
    }

    @Test
    public void startingWithoutACurrentEpisodeResets() throws Exception {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        select(episode.getId());
        assertFull();
    }

    @Test
    public void androidAutoSelectionResetsBeforePrepareOrPlay() throws Exception {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        when(controller.getPackageName()).thenReturn("com.google.android.projection.gearhead");
        setCurrent(Long.MAX_VALUE);
        select(episode.getId());
        assertFull();
        timer.updateRemainingTime(REMAINING_DURATION);
        finished(Player.COMMAND_SET_MEDIA_ITEM, Player.COMMAND_PREPARE);
        service.sessionCallback.onDisconnected(session, controller);
        playRequest(Player.COMMAND_PLAY_PAUSE);
        assertRemaining();
    }

    @Test
    public void sameEpisodeSelectionDoesNotResetWhilePlayingOrPaused() throws Exception {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        for (boolean playing : new boolean[] {false, true}) {
            setCurrent(episode.getId());
            when(player.isPlaying()).thenReturn(playing);
            when(player.getPlayWhenReady()).thenReturn(playing);
            select(episode.getId());
            playRequest(Player.COMMAND_SET_MEDIA_ITEM, Player.COMMAND_PREPARE, Player.COMMAND_PLAY_PAUSE);
            assertRemaining();
        }
    }

    @Test
    public void voiceSelectionResetsForADifferentEpisodeOnly() throws Exception {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        when(controller.getPackageName()).thenReturn("com.google.android.googlequicksearchbox");
        for (String query : new String[] {"New episode", ""}) {
            setCurrent(Long.MAX_VALUE);
            timer.updateRemainingTime(REMAINING_DURATION);
            apply(await(service.sessionCallback.onSetMediaItems(session, controller,
                    Collections.singletonList(voiceRequest(query)), C.INDEX_UNSET, C.TIME_UNSET)));
            assertFull();
            timer.updateRemainingTime(REMAINING_DURATION);
            apply(await(service.sessionCallback.onSetMediaItems(session, controller,
                    Collections.singletonList(voiceRequest(query)), C.INDEX_UNSET, C.TIME_UNSET)));
            assertRemaining();
        }
    }

    @Test
    public void voiceSelectionComparesWithTheEpisodeAtRequestTime() throws Exception {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        setCurrent(Long.MAX_VALUE);
        ListenableFuture<MediaSession.MediaItemsWithStartPosition> selection = service.sessionCallback.onSetMediaItems(
                session, controller, Collections.singletonList(voiceRequest("New episode")),
                C.INDEX_UNSET, C.TIME_UNSET);
        setCurrent(episode.getId());
        await(selection);
        assertFull();
    }

    @Test
    public void delayedVoiceSelectionDoesNotResetANewerTimer() throws Exception {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        var previousIoHandler = RxJavaPlugins.getIoSchedulerHandler();
        try {
            for (boolean initiallyAbsent : new boolean[] {false, true}) {
                setCurrent(Long.MAX_VALUE);
                if (initiallyAbsent) {
                    timer.stop();
                    ReflectionHelpers.setField(service, "sleepTimer", null);
                }
                TestScheduler lookup = new TestScheduler();
                RxJavaPlugins.setIoSchedulerHandler(ignored -> lookup);
                ListenableFuture<MediaSession.MediaItemsWithStartPosition> selection =
                        service.sessionCallback.onSetMediaItems(session, controller,
                                Collections.singletonList(voiceRequest(initiallyAbsent ? "" : "New episode")),
                                C.INDEX_UNSET, C.TIME_UNSET);
                assertFalse(selection.isDone());
                if (!initiallyAbsent) {
                    timer.stop();
                }
                timer = new TestClockSleepTimer(RuntimeEnvironment.getApplication());
                timer.start(FULL_DURATION);
                timer.updateRemainingTime(REMAINING_DURATION);
                ReflectionHelpers.setField(service, "sleepTimer", timer);
                CompletableFuture.runAsync(lookup::triggerActions).get(5, TimeUnit.SECONDS);
                assertEquals(1, await(selection).mediaItems.size());
                assertRemaining();
            }
        } finally {
            RxJavaPlugins.setIoSchedulerHandler(previousIoHandler);
        }
    }

    @Test
    public void failedVoiceSearchAndEmptyRequestsDoNotReset() throws Exception {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        for (MediaItem request : new MediaItem[] {voiceRequest("Unrelated"), MediaItem.EMPTY}) {
            assertTrue(await(service.sessionCallback.onSetMediaItems(session, controller,
                    Collections.singletonList(request), C.INDEX_UNSET, C.TIME_UNSET)).mediaItems.isEmpty());
            assertRemaining();
        }
        assertTrue(await(service.sessionCallback.onSetMediaItems(session, controller,
                Collections.emptyList(), C.INDEX_UNSET, C.TIME_UNSET)).mediaItems.isEmpty());
        assertRemaining();
    }

    @Test
    public void selectionUsesTheRequestedStartIndex() throws Exception {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        FeedMedia other = seedEpisode("Other episode");
        setCurrent(episode.getId());
        List<MediaItem> requests = List.of(MediaItemAdapter.fromMediaIdStub(episode.getId()),
                MediaItemAdapter.fromMediaIdStub(other.getId()));
        await(service.sessionCallback.onSetMediaItems(session, controller, requests, 1, C.TIME_UNSET));
        assertFull();
        timer.updateRemainingTime(REMAINING_DURATION);
        setCurrent(other.getId());
        await(service.sessionCallback.onSetMediaItems(session, controller, requests, 1, C.TIME_UNSET));
        assertRemaining();
    }

    @Test
    public void streamingConfirmationAndCastRestoreDoNotResetAgain() throws Exception {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        select(episode.getId());
        assertFull();
        timer.updateRemainingTime(REMAINING_DURATION);
        when(player.getCurrentMediaItem()).thenReturn(new MediaItem.Builder()
                .setMediaId(MediaItemAdapter.MEDIA_ID_CONFIRM_STREAMING).build());
        confirmStreaming();
        assertRemaining();
        when(player.getCurrentMediaItem()).thenReturn(null);
        apply(await(service.sessionCallback.onSetMediaItems(session, controller,
                Collections.singletonList(internalRequest()), C.INDEX_UNSET, C.TIME_UNSET)));
        playRequest(Player.COMMAND_PLAY_PAUSE);
        assertRemaining();
    }

    @Test
    public void automaticTransitionsNextSkipAndQueueEditsDoNotReset() throws Exception {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        setCurrent(episode.getId());
        listener.onMediaItemTransition(player.getCurrentMediaItem(), Player.MEDIA_ITEM_TRANSITION_REASON_AUTO);
        await(service.sessionCallback.onAddMediaItems(session, controller,
                Collections.singletonList(MediaItemAdapter.fromMediaIdStub(episode.getId()))));
        for (int command : new int[] {Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                Player.COMMAND_CHANGE_MEDIA_ITEMS}) {
            finished(command);
            assertRemaining();
        }
        service.sessionCallback.onCustomCommand(session, controller,
                new SessionCommand("skip_episode", Bundle.EMPTY), Bundle.EMPTY).get();
        assertRemaining();
    }

    @Test
    public void finishedEpisodeAdvancesToQueuedSuccessorWithoutResettingTimer() throws Exception {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        UserPreferences.setFollowQueue(true);
        UserPreferences.setAllowMobileStreaming(true);
        FeedMedia successor = seedEpisode("Queued successor");
        setCurrent(episode.getId());
        ReflectionHelpers.setField(service, "currentPlayable", episode);
        ReflectionHelpers.setField(service, "exoPlayer", mock(ExoPlayer.class));
        SettableFuture<MediaItem> advanced = SettableFuture.create();
        doAnswer(invocation -> {
            advanced.set(invocation.getArgument(0));
            return null;
        }).when(player).setMediaItem(any(MediaItem.class), anyLong());
        AutoDownloadManager previousAutoDownloadManager = AutoDownloadManager.getInstance();
        AutoDownloadManager.setInstance(mock(AutoDownloadManager.class));
        try {
            DBWriter.addQueueItem(RuntimeEnvironment.getApplication(), episode.getItem(), successor.getItem())
                    .get(5, TimeUnit.SECONDS);
            listener.onPlaybackStateChanged(Player.STATE_ENDED);
            assertEquals(String.valueOf(successor.getId()), await(advanced).mediaId);
            verify(player).setPlayWhenReady(true);
            verify(player).prepare();
            FeedMedia current = ReflectionHelpers.getField(service, "currentPlayable");
            assertEquals(successor.getId(), current.getId());
            assertSame(timer, ReflectionHelpers.getField(service, "sleepTimer"));
            assertTrue(timer.isActive());
            assertRemaining();
        } finally {
            DBWriter.tearDownTests();
            AutoDownloadManager.setInstance(previousAutoDownloadManager);
        }
    }

    @Test
    public void pauseResumeSeeksAndSpeedDoNotReset() throws Exception {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        setCurrent(episode.getId());
        finished(Player.COMMAND_PLAY_PAUSE);
        playRequest(Player.COMMAND_PLAY_PAUSE);
        for (int command : new int[] {Player.COMMAND_SEEK_TO_MEDIA_ITEM, Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_DEFAULT_POSITION, Player.COMMAND_SEEK_BACK, Player.COMMAND_SEEK_FORWARD,
                Player.COMMAND_SET_SPEED_AND_PITCH, Player.COMMAND_SET_VOLUME, Player.COMMAND_PREPARE}) {
            finished(command);
            assertRemaining();
        }
        for (String action : new String[] {"rewind", "fast_forward", "playback_speed", "next_chapter"}) {
            service.sessionCallback.onCustomCommand(session, controller,
                    new SessionCommand(action, Bundle.EMPTY), Bundle.EMPTY).get();
            assertRemaining();
        }
    }

    @Test
    public void playbackResumptionDoesNotReset() throws Exception {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        PlaybackPreferences.writeMediaPlaying(episode);
        apply(await(service.sessionCallback.onPlaybackResumption(session, controller)));
        playRequest(Player.COMMAND_PLAY_PAUSE);
        assertRemaining();
    }

    @Test
    public void disabledSettingAndInactiveTimersRemainUnchanged() throws Exception {
        assertFalse(SleepTimerPreferences.resetOnPlaybackInteraction());
        select(episode.getId());
        assertRemaining();
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        for (boolean stopped : new boolean[] {false, true}) {
            if (stopped) {
                timer.stop();
            } else {
                timer.updateRemainingTime(0);
            }
            setCurrent(Long.MAX_VALUE);
            select(episode.getId());
            assertFalse(timer.isActive());
        }
    }

    @Test
    public void episodeTimerIsNotRestarted() throws Exception {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        timer.stop();
        timer = new EpisodeSleepTimer(RuntimeEnvironment.getApplication());
        timer.start(3);
        timer.updateRemainingTime(1);
        ReflectionHelpers.setField(service, "sleepTimer", timer);
        select(episode.getId());
        assertEquals(1, timer.getTimeLeft().getDisplayValue());
    }

    @Test
    public void resetRestoresVolumeAndUsesOriginalDuration() throws Exception {
        SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        timer.updateRemainingTime(5000);
        EventBus.getDefault().postSticky(SleepTimerUpdatedEvent.updated(timer.getTimeLeft()));
        verify(player).setVolume(0.5f);
        select(episode.getId());
        verify(player, atLeastOnce()).setVolume(1.0f);
        assertFull();
        ((TestClockSleepTimer) timer).advanceTime(1000);
        timer.playbackPositionUpdate(new PlaybackPositionEvent(1000, 3600000));
        assertEquals(FULL_DURATION - 1000, timer.getTimeLeft().getMillisValue());
    }

    @Test
    public void repeatedShakesKeepTheListenerActiveWithEitherManualStartSetting() {
        ShakeListener shakes = mock(ShakeListener.class);
        ReflectionHelpers.setField(shakes, "mSleepTimer", timer);
        ReflectionHelpers.setField(timer, "shakeListener", shakes);
        doCallRealMethod().when(shakes).onSensorChanged(any());
        SensorEvent shake = ReflectionHelpers.callConstructor(SensorEvent.class,
                ReflectionHelpers.ClassParameter.from(int.class, 3));
        shake.values[0] = 3 * SensorManager.GRAVITY_EARTH;
        for (boolean enabled : new boolean[] {false, true}) {
            SleepTimerPreferences.setResetOnPlaybackInteraction(enabled);
            timer.updateRemainingTime(5000);
            shakes.onSensorChanged(shake);
            assertFull();
            assertSame(shakes, ReflectionHelpers.getField(timer, "shakeListener"));
            timer.updateRemainingTime(REMAINING_DURATION);
            shakes.onSensorChanged(shake);
            assertFull();
            verify(shakes, never()).pause();
        }
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

    private void select(long id) throws Exception {
        apply(await(service.sessionCallback.onSetMediaItems(session, controller,
                Collections.singletonList(MediaItemAdapter.fromMediaIdStub(id)), C.INDEX_UNSET, C.TIME_UNSET)));
    }

    private void apply(MediaSession.MediaItemsWithStartPosition result) {
        assertFalse(result.mediaItems.isEmpty());
        setCurrent(Long.parseLong(result.mediaItems.get(Math.max(0, result.startIndex)).mediaId));
    }

    private void setCurrent(long id) {
        FeedItem item = new FeedItem();
        item.setFeed(new Feed("url", null, null));
        FeedMedia media = new FeedMedia(item, "http://example.com", 2, "mime");
        media.setId(id);
        item.setMedia(media);
        ReflectionHelpers.setField(service, "currentPlayable", media);
        when(player.getCurrentMediaItem()).thenReturn(MediaItemAdapter.fromMediaIdStub(id));
        when(player.getPlayWhenReady()).thenReturn(false);
        when(player.isPlaying()).thenReturn(false);
    }

    private void playRequest(int... commands) {
        when(player.getPlayWhenReady()).thenReturn(true);
        finished(commands);
    }

    private void finished(int... commands) {
        service.sessionCallback.onPlayerInteractionFinished(session, controller,
                new Player.Commands.Builder().addAll(commands).build());
    }

    private void confirmStreaming() throws Exception {
        MediaItem request = internalRequest();
        MediaSession.ControllerInfo internal = mock(MediaSession.ControllerInfo.class);
        apply(await(service.sessionCallback.onSetMediaItems(session, internal,
                Collections.singletonList(request), C.INDEX_UNSET, C.TIME_UNSET)));
        when(player.getPlayWhenReady()).thenReturn(true);
        service.sessionCallback.onPlayerInteractionFinished(session, internal,
                new Player.Commands.Builder().addAll(Player.COMMAND_SET_MEDIA_ITEM, Player.COMMAND_PREPARE,
                        Player.COMMAND_PLAY_PAUSE).build());
    }

    private MediaItem internalRequest() {
        Bundle extras = new Bundle();
        extras.putBoolean("internal_selection", true);
        return MediaItemAdapter.fromMediaIdStub(episode.getId()).buildUpon()
                .setRequestMetadata(new MediaItem.RequestMetadata.Builder().setExtras(extras).build()).build();
    }

    private MediaItem voiceRequest(String query) {
        return MediaItem.EMPTY.buildUpon().setRequestMetadata(
                new MediaItem.RequestMetadata.Builder().setSearchQuery(query).build()).build();
    }

    private void assertRemaining() {
        assertEquals(REMAINING_DURATION, timer.getTimeLeft().getMillisValue());
    }

    private void assertFull() {
        assertEquals(FULL_DURATION, timer.getTimeLeft().getMillisValue());
        assertEquals(FULL_DURATION,
                EventBus.getDefault().getStickyEvent(SleepTimerUpdatedEvent.class).getMillisTimeLeft());
    }

    private <T> T await(ListenableFuture<T> future) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!future.isDone() && System.nanoTime() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(10);
        }
        return future.get(0, TimeUnit.SECONDS);
    }

    private FeedMedia seedEpisode() {
        return seedEpisode("New episode");
    }

    private FeedMedia seedEpisode(String title) {
        Feed feed = new Feed("url/" + title, null, null);
        feed.setItems(new ArrayList<>());
        FeedItem item = new FeedItem();
        item.setItemIdentifier(title);
        item.setTitle(title);
        item.setMedia(new FeedMedia(item, "http://example.com", 2, "mime"));
        item.setFeed(feed);
        feed.getItems().add(item);
        return FeedDatabaseWriter.updateFeed(RuntimeEnvironment.getApplication(), feed, false)
                .getItems().get(0).getMedia();
    }

    private static class TestClockSleepTimer extends ClockSleepTimer {
        TestClockSleepTimer(Context context) {
            super(context);
        }

        @Override
        public void reset() {
            assertEquals(Looper.getMainLooper(), Looper.myLooper());
            super.reset();
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
