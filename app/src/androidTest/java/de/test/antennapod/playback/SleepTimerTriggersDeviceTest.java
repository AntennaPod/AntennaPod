package de.test.antennapod.playback;

import android.content.ComponentName;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.view.KeyEvent;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionResult;
import androidx.media3.session.SessionToken;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.common.util.concurrent.ListenableFuture;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.event.playback.SleepTimerUpdatedEvent;
import de.danoeh.antennapod.playback.base.MediaItemAdapter;
import de.danoeh.antennapod.playback.service.Media3PlaybackService;
import de.danoeh.antennapod.playback.service.internal.MediaLibrarySessionCallback;
import de.danoeh.antennapod.storage.database.PodDBAdapter;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.preferences.SleepTimerPreferences;
import de.danoeh.antennapod.storage.preferences.SleepTimerType;
import de.danoeh.antennapod.storage.preferences.SynchronizationSettings;
import de.danoeh.antennapod.ui.appstartintent.MediaButtonStarter;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
@OptIn(markerClass = UnstableApi.class)
public class SleepTimerTriggersDeviceTest {
    private static final long FULL = TimeUnit.MINUTES.toMillis(30);
    private static final long REMAINING = TimeUnit.MINUTES.toMillis(10);
    private Context context;
    private MediaController controller;
    private long firstMediaId;
    private long secondMediaId;
    private long thirdMediaId;
    private long chapterMediaId;
    private long chapterStart;

    @Test
    public void allPlaybackTriggersPreservePauseResume() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String backup = InstrumentationRegistry.getArguments().getString("isolatedBackup");
        assumeTrue("Requires a temporary database copy", backup != null);
        assertTrue(backup.matches("codex-sleep-timer-test-[0-9a-f]+"));
        assertEquals("de.danoeh.antennapod.debug", context.getPackageName());
        assertTrue(new File(context.getFilesDir(), backup + "/databases/Antennapod.db").isFile());
        assertFalse("Synchronization must be disconnected", SynchronizationSettings.isProviderConnected());
        assertTrue(SleepTimerPreferences.resetOnPlaybackInteraction());
        assertEquals("30", SleepTimerPreferences.lastTimerValue());
        readMediaIds();
        ParcelFileDescriptor launch = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("am start --activity-clear-task --activity-new-task -n de.danoeh.antennapod.debug/" + MainActivity.class.getName());
        try (InputStream stream = new ParcelFileDescriptor.AutoCloseInputStream(launch)) {
            byte[] buffer = new byte[1024];
            while (stream.read(buffer) != -1) {
            }
        }
        AtomicReference<ListenableFuture<MediaController>> future = new AtomicReference<>();
        main(() -> future.set(new MediaController.Builder(context,
                new SessionToken(context, new ComponentName(context, Media3PlaybackService.class)))
                .setApplicationLooper(Looper.getMainLooper()).buildAsync()));
        controller = future.get().get(20, TimeUnit.SECONDS);
        try {
            checkFailedSelection();
            checkPauseResume();
            resets("episode selection", () -> controller.setMediaItem(
                    MediaItemAdapter.fromMediaIdStub(thirdMediaId)));
            preserves("seek bar", () -> controller.seekTo(180000));
            preserves("restart episode", controller::seekToDefaultPosition);
            resets("playlist episode selection", () -> controller.seekTo(0, 0));
            preserves("rewind button", controller::seekBack);
            preserves("fast-forward button", controller::seekForward);
            preserves("next", controller::seekToNext);
            preserves("next episode", controller::seekToNextMediaItem);
            preserves("previous", controller::seekToPrevious);
            preserves("previous episode", controller::seekToPreviousMediaItem);
            preserves("playback speed", () -> controller.setPlaybackSpeed(1.25f));
            preservesCustom("notification rewind", "rewind");
            preservesCustom("notification fast-forward", "fast_forward");
            preservesCustom("notification skip episode", "skip_episode");
            preservesCustom("notification playback speed", "playback_speed");
            checkChapter();
            checkHardware(KeyEvent.KEYCODE_MEDIA_REWIND, "hardware rewind");
            checkHardware(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, "hardware fast-forward");
            checkHardware(KeyEvent.KEYCODE_MEDIA_NEXT, "hardware next episode");
            checkHardware(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "hardware previous episode");
            checkHardwarePauseResume();
            preparePlaylist();
            primeTimer();
            main(() -> {
                try {
                    MediaButtonStarter.createPendingIntent(context, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM).send();
                } catch (Exception error) {
                    throw new AssertionError(error);
                }
            });
            Thread.sleep(300);
            assertRemaining("widget next episode");
            report("widget next episode", remaining());
            preserves("queue edit", () -> controller.addMediaItem(MediaItemAdapter.fromMediaIdStub(firstMediaId)));
            preserves("volume", () -> controller.setVolume(0.4f));
            preserves("prepare", controller::prepare);
            preserves("empty playlist", controller::clearMediaItems);
            checkAutomaticTransition();
            checkDisabledSetting();
            checkDisabledAndExpiredTimer();
            checkFadeReset();
            checkEpisodeTimer();
        } finally {
            command(MediaLibrarySessionCallback.SESSION_COMMAND_DISABLE_SLEEP_TIMER, Bundle.EMPTY);
            main(() -> {
                controller.pause();
                controller.release();
            });
        }
    }

    private void readMediaIds() {
        try (SQLiteDatabase database = SQLiteDatabase.openDatabase(
                context.getDatabasePath(PodDBAdapter.DATABASE_NAME).getPath(), null, SQLiteDatabase.OPEN_READONLY);
             Cursor media = database.rawQuery("SELECT id FROM FeedMedia WHERE downloaded > 0 ORDER BY id LIMIT 3", null);
             Cursor chapter = database.rawQuery("SELECT m.id, MIN(c.start) FROM FeedMedia m "
                     + "JOIN SimpleChapters c ON c.feeditem = m.feeditem "
                     + "WHERE m.downloaded > 0 AND c.start > 0 GROUP BY m.id LIMIT 1", null)) {
            assertTrue(media.moveToNext());
            firstMediaId = media.getLong(0);
            assertTrue(media.moveToNext());
            secondMediaId = media.getLong(0);
            assertTrue(media.moveToNext());
            thirdMediaId = media.getLong(0);
            assertTrue("Requires a downloaded episode with chapters", chapter.moveToFirst());
            chapterMediaId = chapter.getLong(0);
            chapterStart = chapter.getLong(1);
        }
    }

    private void preparePlaylist() throws Exception {
        main(() -> {
            controller.pause();
            controller.setPlaybackSpeed(1f);
            controller.setMediaItems(Arrays.asList(MediaItemAdapter.fromMediaIdStub(firstMediaId),
                    MediaItemAdapter.fromMediaIdStub(secondMediaId),
                    MediaItemAdapter.fromMediaIdStub(thirdMediaId)), 1, 0);
            controller.prepare();
        });
        await(() -> controller.getMediaItemCount() == 3 && controller.getCurrentMediaItemIndex() == 1
                && controller.getPlaybackState() == Player.STATE_READY);
    }

    private void primeTimer() throws Exception {
        command(MediaLibrarySessionCallback.SESSION_COMMAND_SET_SLEEP_TIMER, Bundle.EMPTY);
        command(MediaLibrarySessionCallback.SESSION_COMMAND_EXTEND_SLEEP_TIMER,
                MediaLibrarySessionCallback.createBundle(-TimeUnit.MINUTES.toMillis(20)));
        publishPosition();
        assertRemaining("timer baseline");
    }

    private void publishPosition() {
        main(() -> EventBus.getDefault().post(new PlaybackPositionEvent((int) controller.getCurrentPosition(),
                (int) controller.getDuration())));
    }

    private void resets(String label, Runnable action) throws Exception {
        preparePlaylist();
        primeTimer();
        main(action);
        assertReset(label);
    }

    private void preservesCustom(String label, String action) throws Exception {
        preparePlaylist();
        primeTimer();
        command(new SessionCommand(action, Bundle.EMPTY), Bundle.EMPTY);
        Thread.sleep(300);
        assertRemaining(label);
        report(label, remaining());
    }

    private void preserves(String label, Runnable action) throws Exception {
        preparePlaylist();
        primeTimer();
        main(action);
        Thread.sleep(300);
        assertRemaining(label);
        report(label, remaining());
    }

    private void checkFailedSelection() throws Exception {
        MediaItem search = MediaItem.EMPTY.buildUpon().setRequestMetadata(new MediaItem.RequestMetadata.Builder()
                .setSearchQuery("codex-no-matching-episode-fb0e3d35").build()).build();
        for (MediaItem request : Arrays.asList(search, MediaItemAdapter.fromMediaIdStub(Long.MAX_VALUE))) {
            preparePlaylist();
            primeTimer();
            main(() -> controller.setMediaItem(request));
            await(() -> controller.getMediaItemCount() == 0);
            assertRemaining("failed episode selection");
            report(request == search ? "unmatched voice search" : "missing episode selection", remaining());
        }
    }

    private void checkPauseResume() throws Exception {
        preparePlaylist();
        primeTimer();
        for (int i = 0; i < 2; i++) {
            main(controller::play);
            await(controller::isPlaying);
            assertRemaining("resume");
            main(controller::pause);
            await(() -> !controller.getPlayWhenReady());
            assertRemaining("pause");
        }
        report("pause/resume repeated twice", remaining());
    }

    private void checkChapter() throws Exception {
        main(() -> {
            controller.setMediaItem(MediaItemAdapter.fromMediaIdStub(chapterMediaId));
            controller.prepare();
        });
        await(() -> controller.getCurrentMediaItem() != null
                && controller.getCurrentMediaItem().mediaId.equals(String.valueOf(chapterMediaId))
                && controller.getPlaybackState() == Player.STATE_READY);
        primeTimer();
        main(() -> controller.seekTo(chapterStart));
        assertRemaining("chapter selection");
        report("chapter selection", remaining());
        main(() -> controller.seekTo(0));
        primeTimer();
        command(new SessionCommand("next_chapter", Bundle.EMPTY), Bundle.EMPTY);
        assertRemaining("next chapter command");
        report("next chapter command", remaining());
        await(() -> controller.getCurrentPosition() >= chapterStart);
    }

    private void checkHardware(int keyCode, String label) throws Exception {
        preparePlaylist();
        main(controller::play);
        await(controller::isPlaying);
        primeTimer();
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        main(() -> audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode)));
        Thread.sleep(300);
        assertRemaining(label);
        report(label, remaining());
        primeTimer();
        main(() -> audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode)));
        Thread.sleep(300);
        assertRemaining(label + " key-up");
        report(label + " key-up", remaining());
        main(controller::pause);
    }

    private void prepareQueue() throws Exception {
        DBWriter.clearQueue().get(10, TimeUnit.SECONDS);
        DBWriter.addQueueItem(context, DBReader.getFeedMedia(firstMediaId).getItem(),
                DBReader.getFeedMedia(secondMediaId).getItem()).get(10, TimeUnit.SECONDS);
        main(() -> {
            controller.pause();
            controller.setMediaItem(MediaItemAdapter.fromMediaIdStub(firstMediaId));
            controller.prepare();
        });
        await(() -> controller.getCurrentMediaItem() != null
                && controller.getCurrentMediaItem().mediaId.equals(String.valueOf(firstMediaId))
                && controller.getPlaybackState() == Player.STATE_READY);
    }

    private void checkAutomaticTransition() throws Exception {
        prepareQueue();
        main(() -> controller.seekTo(controller.getDuration() - 1000));
        primeTimer();
        main(controller::play);
        await(() -> controller.getCurrentMediaItem() != null
                && controller.getCurrentMediaItem().mediaId.equals(String.valueOf(secondMediaId)));
        assertRemaining("automatic episode transition");
        report("automatic episode transition", remaining());
        main(controller::pause);
    }

    private void checkDisabledSetting() throws Exception {
        SleepTimerPreferences.setResetOnPlaybackInteraction(false);
        try {
            preparePlaylist();
            primeTimer();
            main(() -> controller.seekTo(0, 0));
            Thread.sleep(300);
            assertRemaining("setting disabled");
            report("setting disabled", remaining());
        } finally {
            SleepTimerPreferences.setResetOnPlaybackInteraction(true);
        }
    }

    private void checkDisabledAndExpiredTimer() throws Exception {
        preparePlaylist();
        command(MediaLibrarySessionCallback.SESSION_COMMAND_DISABLE_SLEEP_TIMER, Bundle.EMPTY);
        main(() -> controller.seekTo(0, 0));
        Thread.sleep(300);
        assertTrue(EventBus.getDefault().getStickyEvent(SleepTimerUpdatedEvent.class).isCancelled());
        report("disabled timer stays disabled", 0);
        command(MediaLibrarySessionCallback.SESSION_COMMAND_SET_SLEEP_TIMER, Bundle.EMPTY);
        command(MediaLibrarySessionCallback.SESSION_COMMAND_EXTEND_SLEEP_TIMER,
                MediaLibrarySessionCallback.createBundle(-FULL));
        publishPosition();
        main(() -> controller.seekTo(0, 0));
        Thread.sleep(300);
        assertTrue(EventBus.getDefault().getStickyEvent(SleepTimerUpdatedEvent.class).isCancelled());
        report("expired timer stays disabled", 0);
    }

    private void checkHardwarePauseResume() throws Exception {
        preparePlaylist();
        main(controller::play);
        await(controller::isPlaying);
        primeTimer();
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        for (int i = 0; i < 2; i++) {
            main(() -> {
                audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
            });
            if (i == 0) {
                await(() -> !controller.getPlayWhenReady());
            } else {
                await(controller::isPlaying);
            }
            assertRemaining("hardware pause/resume");
        }
        report("hardware pause/resume", remaining());
        main(controller::pause);
    }

    private void checkFadeReset() throws Exception {
        preparePlaylist();
        command(MediaLibrarySessionCallback.SESSION_COMMAND_SET_SLEEP_TIMER, Bundle.EMPTY);
        command(MediaLibrarySessionCallback.SESSION_COMMAND_EXTEND_SLEEP_TIMER,
                MediaLibrarySessionCallback.createBundle(-(FULL - 5000)));
        publishPosition();
        await(() -> controller.getVolume() < 0.6f);
        main(() -> controller.seekTo(0, 0));
        assertReset("reset during fade");
        await(() -> controller.getVolume() > 0.99f);
        report("volume restored after reset", remaining());
    }

    private void checkEpisodeTimer() throws Exception {
        preparePlaylist();
        SleepTimerPreferences.setSleepTimerType(SleepTimerType.EPISODES);
        SleepTimerPreferences.setLastTimer("3");
        TimerLifecycleRecorder recorder = new TimerLifecycleRecorder();
        main(() -> EventBus.getDefault().register(recorder));
        try {
            command(MediaLibrarySessionCallback.SESSION_COMMAND_SET_SLEEP_TIMER, Bundle.EMPTY);
            assertEquals(1, recorder.enables);
            int cancellations = recorder.cancellations;
            main(() -> controller.seekTo(0, 0));
            main(() -> controller.setPlaybackSpeed(1.25f));
            command(new SessionCommand("rewind", Bundle.EMPTY), Bundle.EMPTY);
            Thread.sleep(300);
            assertEquals(1, recorder.enables);
            assertEquals(cancellations, recorder.cancellations);
            assertEquals(3, EventBus.getDefault().getStickyEvent(SleepTimerUpdatedEvent.class).getDisplayTimeLeft());
            report("episode mode excluded from interaction resets", 3);
        } finally {
            main(() -> EventBus.getDefault().unregister(recorder));
            SleepTimerPreferences.setSleepTimerType(SleepTimerType.CLOCK);
            SleepTimerPreferences.setLastTimer("30");
        }
    }

    public static class TimerLifecycleRecorder {
        int enables;
        int cancellations;

        @Subscribe(threadMode = ThreadMode.MAIN)
        public void updated(SleepTimerUpdatedEvent event) {
            if (event.wasJustEnabled()) {
                enables++;
            }
            if (event.isCancelled()) {
                cancellations++;
            }
        }
    }

    private void command(SessionCommand command, Bundle args) throws Exception {
        AtomicReference<ListenableFuture<SessionResult>> future = new AtomicReference<>();
        main(() -> future.set(controller.sendCustomCommand(command, args)));
        assertEquals(SessionResult.RESULT_SUCCESS, future.get().get(10, TimeUnit.SECONDS).resultCode);
    }

    private void main(Runnable action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            AtomicReference<Boolean> matched = new AtomicReference<>(false);
            main(() -> matched.set(condition.getAsBoolean()));
            if (matched.get()) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Timed out waiting for playback state");
    }

    private long remaining() {
        return EventBus.getDefault().getStickyEvent(SleepTimerUpdatedEvent.class).getMillisTimeLeft();
    }

    private void assertReset(String label) throws Exception {
        await(() -> remaining() >= FULL - 2000 && remaining() <= FULL);
        assertTrue(label, remaining() >= FULL - 2000);
        report(label, remaining());
    }

    private void assertRemaining(String label) {
        long left = remaining();
        assertTrue(label + ": " + left, left >= REMAINING - 5000 && left <= REMAINING);
    }

    private void report(String label, long left) {
        Bundle result = new Bundle();
        result.putString("stream", "TRIGGER PASS: " + label + "; remaining=" + left + "\n");
        InstrumentationRegistry.getInstrumentation().sendStatus(0, result);
    }
}
