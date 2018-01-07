package de.test.antennapod.service.playback;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.test.InstrumentationTestCase;

import junit.framework.AssertionFailedError;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.service.playback.LocalPSMP;
import de.danoeh.antennapod.core.service.playback.PlaybackServiceMediaPlayer;
import de.danoeh.antennapod.core.service.playback.PlayerStatus;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.test.antennapod.util.service.download.HTTPBin;

/**
 * Test class for LocalPSMP
 */
public class PlaybackServiceMediaPlayerTest extends InstrumentationTestCase {
    private static final String TAG = "PlaybackServiceMediaPlayerTest";

    private static final String PLAYABLE_FILE_URL = "http://127.0.0.1:" + HTTPBin.PORT + "/files/0";
    private static final String PLAYABLE_DEST_URL = "psmptestfile.mp3";
    private String PLAYABLE_LOCAL_URL = null;
    private static final int LATCH_TIMEOUT_SECONDS = 10;

    private HTTPBin httpServer;

    private volatile AssertionFailedError assertionError;

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        PodDBAdapter.deleteDatabase();
        httpServer.stop();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assertionError = null;

        final Context context = getInstrumentation().getTargetContext();

        // create new database
        PodDBAdapter.init(context);
        PodDBAdapter.deleteDatabase();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.close();

        httpServer = new HTTPBin();
        httpServer.start();

        File cacheDir = context.getExternalFilesDir("testFiles");
        if (cacheDir == null)
            cacheDir = context.getExternalFilesDir("testFiles");
        File dest = new File(cacheDir, PLAYABLE_DEST_URL);

        assertNotNull(cacheDir);
        assertTrue(cacheDir.canWrite());
        assertTrue(cacheDir.canRead());
        if (!dest.exists()) {
            InputStream i = getInstrumentation().getContext().getAssets().open("testfile.mp3");
            OutputStream o = new FileOutputStream(new File(cacheDir, PLAYABLE_DEST_URL));
            IOUtils.copy(i, o);
            o.flush();
            o.close();
            i.close();
        }
        PLAYABLE_LOCAL_URL = "file://" + dest.getAbsolutePath();
        assertEquals(0, httpServer.serveFile(dest));
    }

    private void checkPSMPInfo(LocalPSMP.PSMPInfo info) {
        try {
            switch (info.playerStatus) {
                case PLAYING:
                case PAUSED:
                case PREPARED:
                case PREPARING:
                case INITIALIZED:
                case INITIALIZING:
                case SEEKING:
                    assertNotNull(info.playable);
                    break;
                case STOPPED:
                    assertNull(info.playable);
                    break;
                case ERROR:
                    assertNull(info.playable);
            }
        } catch (AssertionFailedError e) {
            if (assertionError == null)
                assertionError = e;
        }
    }

    public void testInit() {
        final Context c = getInstrumentation().getTargetContext();
        PlaybackServiceMediaPlayer psmp = new LocalPSMP(c, new DefaultPSMPCallback());
        psmp.shutdown();
    }

    private Playable writeTestPlayable(String downloadUrl, String fileUrl) {
        final Context c = getInstrumentation().getTargetContext();
        Feed f = new Feed(0, null, "f", "l", "d", null, null, null, null, "i", null, null, "l", false);
        FeedPreferences prefs = new FeedPreferences(f.getId(), false, FeedPreferences.AutoDeleteAction.NO, null, null);
        f.setPreferences(prefs);
        f.setItems(new ArrayList<>());
        FeedItem i = new FeedItem(0, "t", "i", "l", new Date(), FeedItem.UNPLAYED, f);
        f.getItems().add(i);
        FeedMedia media = new FeedMedia(0, i, 0, 0, 0, "audio/wav", fileUrl, downloadUrl, fileUrl != null, null, 0, 0);
        i.setMedia(media);
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(f);
        assertTrue(media.getId() != 0);
        adapter.close();
        return media;
    }


    public void testPlayMediaObjectStreamNoStartNoPrepare() throws InterruptedException {
        final Context c = getInstrumentation().getTargetContext();
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        PlaybackServiceMediaPlayer.PSMPCallback callback = new DefaultPSMPCallback() {
            @Override
            public void statusChanged(LocalPSMP.PSMPInfo newInfo) {
                try {
                    checkPSMPInfo(newInfo);
                    if (newInfo.playerStatus == PlayerStatus.ERROR)
                        throw new IllegalStateException("MediaPlayer error");
                    if (countDownLatch.getCount() == 0) {
                        fail();
                    } else if (countDownLatch.getCount() == 2) {
                        assertEquals(PlayerStatus.INITIALIZING, newInfo.playerStatus);
                        countDownLatch.countDown();
                    } else {
                        assertEquals(PlayerStatus.INITIALIZED, newInfo.playerStatus);
                        countDownLatch.countDown();
                    }
                } catch (AssertionFailedError e) {
                    if (assertionError == null)
                        assertionError = e;
                }
            }
        };
        PlaybackServiceMediaPlayer psmp = new LocalPSMP(c, callback);
        Playable p = writeTestPlayable(PLAYABLE_FILE_URL, null);
        psmp.playMediaObject(p, true, false, false);
        boolean res = countDownLatch.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (assertionError != null)
            throw assertionError;
        assertTrue(res);

        assertTrue(psmp.getPSMPInfo().playerStatus == PlayerStatus.INITIALIZED);
        assertFalse(psmp.isStartWhenPrepared());
        psmp.shutdown();
    }

    public void testPlayMediaObjectStreamStartNoPrepare() throws InterruptedException {
        final Context c = getInstrumentation().getTargetContext();
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        PlaybackServiceMediaPlayer.PSMPCallback callback = new DefaultPSMPCallback() {
            @Override
            public void statusChanged(LocalPSMP.PSMPInfo newInfo) {
                try {
                    checkPSMPInfo(newInfo);
                    if (newInfo.playerStatus == PlayerStatus.ERROR)
                        throw new IllegalStateException("MediaPlayer error");
                    if (countDownLatch.getCount() == 0) {
                        fail();
                    } else if (countDownLatch.getCount() == 2) {
                        assertEquals(PlayerStatus.INITIALIZING, newInfo.playerStatus);
                        countDownLatch.countDown();
                    } else {
                        assertEquals(PlayerStatus.INITIALIZED, newInfo.playerStatus);
                        countDownLatch.countDown();
                    }
                } catch (AssertionFailedError e) {
                    if (assertionError == null)
                        assertionError = e;
                }
            }
        };
        PlaybackServiceMediaPlayer psmp = new LocalPSMP(c, callback);
        Playable p = writeTestPlayable(PLAYABLE_FILE_URL, null);
        psmp.playMediaObject(p, true, true, false);

        boolean res = countDownLatch.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (assertionError != null)
            throw assertionError;
        assertTrue(res);

        assertTrue(psmp.getPSMPInfo().playerStatus == PlayerStatus.INITIALIZED);
        assertTrue(psmp.isStartWhenPrepared());
        psmp.shutdown();
    }

    public void testPlayMediaObjectStreamNoStartPrepare() throws InterruptedException {
        final Context c = getInstrumentation().getTargetContext();
        final CountDownLatch countDownLatch = new CountDownLatch(4);
        PlaybackServiceMediaPlayer.PSMPCallback callback = new DefaultPSMPCallback() {
            @Override
            public void statusChanged(LocalPSMP.PSMPInfo newInfo) {
                try {
                    checkPSMPInfo(newInfo);
                    if (newInfo.playerStatus == PlayerStatus.ERROR)
                        throw new IllegalStateException("MediaPlayer error");
                    if (countDownLatch.getCount() == 0) {
                        fail();
                    } else if (countDownLatch.getCount() == 4) {
                        assertEquals(PlayerStatus.INITIALIZING, newInfo.playerStatus);
                    } else if (countDownLatch.getCount() == 3) {
                        assertEquals(PlayerStatus.INITIALIZED, newInfo.playerStatus);
                    } else if (countDownLatch.getCount() == 2) {
                        assertEquals(PlayerStatus.PREPARING, newInfo.playerStatus);
                    } else if (countDownLatch.getCount() == 1) {
                        assertEquals(PlayerStatus.PREPARED, newInfo.playerStatus);
                    }
                    countDownLatch.countDown();
                } catch (AssertionFailedError e) {
                    if (assertionError == null)
                        assertionError = e;
                }
            }
        };
        PlaybackServiceMediaPlayer psmp = new LocalPSMP(c, callback);
        Playable p = writeTestPlayable(PLAYABLE_FILE_URL, null);
        psmp.playMediaObject(p, true, false, true);
        boolean res = countDownLatch.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (assertionError != null)
            throw assertionError;
        assertTrue(res);
        assertTrue(psmp.getPSMPInfo().playerStatus == PlayerStatus.PREPARED);

        psmp.shutdown();
    }

    public void testPlayMediaObjectStreamStartPrepare() throws InterruptedException {
        final Context c = getInstrumentation().getTargetContext();
        final CountDownLatch countDownLatch = new CountDownLatch(5);
        PlaybackServiceMediaPlayer.PSMPCallback callback = new DefaultPSMPCallback() {
            @Override
            public void statusChanged(LocalPSMP.PSMPInfo newInfo) {
                try {
                    checkPSMPInfo(newInfo);
                    if (newInfo.playerStatus == PlayerStatus.ERROR)
                        throw new IllegalStateException("MediaPlayer error");
                    if (countDownLatch.getCount() == 0) {
                        fail();

                    } else if (countDownLatch.getCount() == 5) {
                        assertEquals(PlayerStatus.INITIALIZING, newInfo.playerStatus);
                    } else if (countDownLatch.getCount() == 4) {
                        assertEquals(PlayerStatus.INITIALIZED, newInfo.playerStatus);
                    } else if (countDownLatch.getCount() == 3) {
                        assertEquals(PlayerStatus.PREPARING, newInfo.playerStatus);
                    } else if (countDownLatch.getCount() == 2) {
                        assertEquals(PlayerStatus.PREPARED, newInfo.playerStatus);
                    } else if (countDownLatch.getCount() == 1) {
                        assertEquals(PlayerStatus.PLAYING, newInfo.playerStatus);
                    }
                    countDownLatch.countDown();
                } catch (AssertionFailedError e) {
                    if (assertionError == null)
                        assertionError = e;
                }
            }
        };
        PlaybackServiceMediaPlayer psmp = new LocalPSMP(c, callback);
        Playable p = writeTestPlayable(PLAYABLE_FILE_URL, null);
        psmp.playMediaObject(p, true, true, true);
        boolean res = countDownLatch.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (assertionError != null)
            throw assertionError;
        assertTrue(res);
        assertTrue(psmp.getPSMPInfo().playerStatus == PlayerStatus.PLAYING);
        psmp.shutdown();
    }

    public void testPlayMediaObjectLocalNoStartNoPrepare() throws InterruptedException {
        final Context c = getInstrumentation().getTargetContext();
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        PlaybackServiceMediaPlayer.PSMPCallback callback = new DefaultPSMPCallback() {
            @Override
            public void statusChanged(LocalPSMP.PSMPInfo newInfo) {
                try {
                    checkPSMPInfo(newInfo);
                    if (newInfo.playerStatus == PlayerStatus.ERROR)
                        throw new IllegalStateException("MediaPlayer error");
                    if (countDownLatch.getCount() == 0) {
                        fail();
                    } else if (countDownLatch.getCount() == 2) {
                        assertEquals(PlayerStatus.INITIALIZING, newInfo.playerStatus);
                        countDownLatch.countDown();
                    } else {
                        assertEquals(PlayerStatus.INITIALIZED, newInfo.playerStatus);
                        countDownLatch.countDown();
                    }
                } catch (AssertionFailedError e) {
                    if (assertionError == null)
                        assertionError = e;
                }
            }
        };
        PlaybackServiceMediaPlayer psmp = new LocalPSMP(c, callback);
        Playable p = writeTestPlayable(PLAYABLE_FILE_URL, PLAYABLE_LOCAL_URL);
        psmp.playMediaObject(p, false, false, false);
        boolean res = countDownLatch.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (assertionError != null)
            throw assertionError;
        assertTrue(res);
        assertTrue(psmp.getPSMPInfo().playerStatus == PlayerStatus.INITIALIZED);
        assertFalse(psmp.isStartWhenPrepared());
        psmp.shutdown();
    }

    public void testPlayMediaObjectLocalStartNoPrepare() throws InterruptedException {
        final Context c = getInstrumentation().getTargetContext();
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        PlaybackServiceMediaPlayer.PSMPCallback callback = new DefaultPSMPCallback() {
            @Override
            public void statusChanged(LocalPSMP.PSMPInfo newInfo) {
                try {
                    checkPSMPInfo(newInfo);
                    if (newInfo.playerStatus == PlayerStatus.ERROR)
                        throw new IllegalStateException("MediaPlayer error");
                    if (countDownLatch.getCount() == 0) {
                        fail();
                    } else if (countDownLatch.getCount() == 2) {
                        assertEquals(PlayerStatus.INITIALIZING, newInfo.playerStatus);
                        countDownLatch.countDown();
                    } else {
                        assertEquals(PlayerStatus.INITIALIZED, newInfo.playerStatus);
                        countDownLatch.countDown();
                    }
                } catch (AssertionFailedError e) {
                    if (assertionError == null)
                        assertionError = e;
                }
            }
        };
        PlaybackServiceMediaPlayer psmp = new LocalPSMP(c, callback);
        Playable p = writeTestPlayable(PLAYABLE_FILE_URL, PLAYABLE_LOCAL_URL);
        psmp.playMediaObject(p, false, true, false);
        boolean res = countDownLatch.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (assertionError != null)
            throw assertionError;
        assertTrue(res);
        assertTrue(psmp.getPSMPInfo().playerStatus == PlayerStatus.INITIALIZED);
        assertTrue(psmp.isStartWhenPrepared());
        psmp.shutdown();
    }

    public void testPlayMediaObjectLocalNoStartPrepare() throws InterruptedException {
        final Context c = getInstrumentation().getTargetContext();
        final CountDownLatch countDownLatch = new CountDownLatch(4);
        PlaybackServiceMediaPlayer.PSMPCallback callback = new DefaultPSMPCallback() {
            @Override
            public void statusChanged(LocalPSMP.PSMPInfo newInfo) {
                try {
                    checkPSMPInfo(newInfo);
                    if (newInfo.playerStatus == PlayerStatus.ERROR)
                        throw new IllegalStateException("MediaPlayer error");
                    if (countDownLatch.getCount() == 0) {
                        fail();
                    } else if (countDownLatch.getCount() == 4) {
                        assertEquals(PlayerStatus.INITIALIZING, newInfo.playerStatus);
                    } else if (countDownLatch.getCount() == 3) {
                        assertEquals(PlayerStatus.INITIALIZED, newInfo.playerStatus);
                    } else if (countDownLatch.getCount() == 2) {
                        assertEquals(PlayerStatus.PREPARING, newInfo.playerStatus);
                    } else if (countDownLatch.getCount() == 1) {
                        assertEquals(PlayerStatus.PREPARED, newInfo.playerStatus);
                    }
                    countDownLatch.countDown();
                } catch (AssertionFailedError e) {
                    if (assertionError == null)
                        assertionError = e;
                }
            }
        };
        PlaybackServiceMediaPlayer psmp = new LocalPSMP(c, callback);
        Playable p = writeTestPlayable(PLAYABLE_FILE_URL, PLAYABLE_LOCAL_URL);
        psmp.playMediaObject(p, false, false, true);
        boolean res = countDownLatch.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (assertionError != null)
            throw assertionError;
        assertTrue(res);
        assertTrue(psmp.getPSMPInfo().playerStatus == PlayerStatus.PREPARED);
        psmp.shutdown();
    }

    public void testPlayMediaObjectLocalStartPrepare() throws InterruptedException {
        final Context c = getInstrumentation().getTargetContext();
        final CountDownLatch countDownLatch = new CountDownLatch(5);
        PlaybackServiceMediaPlayer.PSMPCallback callback = new DefaultPSMPCallback() {
            @Override
            public void statusChanged(LocalPSMP.PSMPInfo newInfo) {
                try {
                    checkPSMPInfo(newInfo);
                    if (newInfo.playerStatus == PlayerStatus.ERROR)
                        throw new IllegalStateException("MediaPlayer error");
                    if (countDownLatch.getCount() == 0) {
                        fail();
                    } else if (countDownLatch.getCount() == 5) {
                        assertEquals(PlayerStatus.INITIALIZING, newInfo.playerStatus);
                    } else if (countDownLatch.getCount() == 4) {
                        assertEquals(PlayerStatus.INITIALIZED, newInfo.playerStatus);
                    } else if (countDownLatch.getCount() == 3) {
                        assertEquals(PlayerStatus.PREPARING, newInfo.playerStatus);
                    } else if (countDownLatch.getCount() == 2) {
                        assertEquals(PlayerStatus.PREPARED, newInfo.playerStatus);
                    } else if (countDownLatch.getCount() == 1) {
                        assertEquals(PlayerStatus.PLAYING, newInfo.playerStatus);
                    }

                } catch (AssertionFailedError e) {
                    if (assertionError == null)
                        assertionError = e;
                } finally {
                    countDownLatch.countDown();
                }
            }
        };
        PlaybackServiceMediaPlayer psmp = new LocalPSMP(c, callback);
        Playable p = writeTestPlayable(PLAYABLE_FILE_URL, PLAYABLE_LOCAL_URL);
        psmp.playMediaObject(p, false, true, true);
        boolean res = countDownLatch.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (assertionError != null)
            throw assertionError;
        assertTrue(res);
        assertTrue(psmp.getPSMPInfo().playerStatus == PlayerStatus.PLAYING);
        psmp.shutdown();
    }

    private void pauseTestSkeleton(final PlayerStatus initialState, final boolean stream, final boolean abandonAudioFocus, final boolean reinit, long timeoutSeconds) throws InterruptedException {
        final Context c = getInstrumentation().getTargetContext();
        final int latchCount = (stream && reinit) ? 2 : 1;
        final CountDownLatch countDownLatch = new CountDownLatch(latchCount);

        PlaybackServiceMediaPlayer.PSMPCallback callback = new DefaultPSMPCallback() {
            @Override
            public void statusChanged(LocalPSMP.PSMPInfo newInfo) {
                checkPSMPInfo(newInfo);
                if (newInfo.playerStatus == PlayerStatus.ERROR) {
                    if (assertionError == null)
                        assertionError = new UnexpectedStateChange(newInfo.playerStatus);
                } else if (initialState != PlayerStatus.PLAYING) {
                    if (assertionError == null)
                        assertionError = new UnexpectedStateChange(newInfo.playerStatus);
                } else {
                    switch (newInfo.playerStatus) {
                        case PAUSED:
                            if (latchCount == countDownLatch.getCount())
                                countDownLatch.countDown();
                            else {
                                if (assertionError == null)
                                    assertionError = new UnexpectedStateChange(newInfo.playerStatus);
                            }
                            break;
                        case INITIALIZED:
                            if (stream && reinit && countDownLatch.getCount() < latchCount) {
                                countDownLatch.countDown();
                            } else if (countDownLatch.getCount() < latchCount) {
                                if (assertionError == null)
                                    assertionError = new UnexpectedStateChange(newInfo.playerStatus);
                            }
                            break;
                    }
                }

            }

            @Override
            public void shouldStop() {
                if (assertionError == null)
                    assertionError = new AssertionFailedError("Unexpected call to shouldStop");
            }

            @Override
            public boolean onMediaPlayerError(Object inObj, int what, int extra) {
                if (assertionError == null)
                    assertionError = new AssertionFailedError("Unexpected call to onMediaPlayerError");
                return false;
            }
        };
        PlaybackServiceMediaPlayer psmp = new LocalPSMP(c, callback);
        Playable p = writeTestPlayable(PLAYABLE_FILE_URL, PLAYABLE_LOCAL_URL);
        if (initialState == PlayerStatus.PLAYING) {
            psmp.playMediaObject(p, stream, true, true);
        }
        psmp.pause(abandonAudioFocus, reinit);
        boolean res = countDownLatch.await(timeoutSeconds, TimeUnit.SECONDS);
        if (assertionError != null)
            throw assertionError;
        assertTrue(res || initialState != PlayerStatus.PLAYING);
        psmp.shutdown();
    }

    public void testPauseDefaultState() throws InterruptedException {
        pauseTestSkeleton(PlayerStatus.STOPPED, false, false, false, 1);
    }

    public void testPausePlayingStateNoAbandonNoReinitNoStream() throws InterruptedException {
        pauseTestSkeleton(PlayerStatus.PLAYING, false, false, false, LATCH_TIMEOUT_SECONDS);
    }

    public void testPausePlayingStateNoAbandonNoReinitStream() throws InterruptedException {
        pauseTestSkeleton(PlayerStatus.PLAYING, true, false, false, LATCH_TIMEOUT_SECONDS);
    }

    public void testPausePlayingStateAbandonNoReinitNoStream() throws InterruptedException {
        pauseTestSkeleton(PlayerStatus.PLAYING, false, true, false, LATCH_TIMEOUT_SECONDS);
    }

    public void testPausePlayingStateAbandonNoReinitStream() throws InterruptedException {
        pauseTestSkeleton(PlayerStatus.PLAYING, true, true, false, LATCH_TIMEOUT_SECONDS);
    }

    public void testPausePlayingStateNoAbandonReinitNoStream() throws InterruptedException {
        pauseTestSkeleton(PlayerStatus.PLAYING, false, false, true, LATCH_TIMEOUT_SECONDS);
    }

    public void testPausePlayingStateNoAbandonReinitStream() throws InterruptedException {
        pauseTestSkeleton(PlayerStatus.PLAYING, true, false, true, LATCH_TIMEOUT_SECONDS);
    }

    public void testPausePlayingStateAbandonReinitNoStream() throws InterruptedException {
        pauseTestSkeleton(PlayerStatus.PLAYING, false, true, true, LATCH_TIMEOUT_SECONDS);
    }

    public void testPausePlayingStateAbandonReinitStream() throws InterruptedException {
        pauseTestSkeleton(PlayerStatus.PLAYING, true, true, true, LATCH_TIMEOUT_SECONDS);
    }

    private void resumeTestSkeleton(final PlayerStatus initialState, long timeoutSeconds) throws InterruptedException {
        final Context c = getInstrumentation().getTargetContext();
        final int latchCount = (initialState == PlayerStatus.PAUSED || initialState == PlayerStatus.PLAYING) ? 2 :
                (initialState == PlayerStatus.PREPARED) ? 1 : 0;
        final CountDownLatch countDownLatch = new CountDownLatch(latchCount);

        PlaybackServiceMediaPlayer.PSMPCallback callback = new DefaultPSMPCallback() {
            @Override
            public void statusChanged(LocalPSMP.PSMPInfo newInfo) {
                checkPSMPInfo(newInfo);
                if (newInfo.playerStatus == PlayerStatus.ERROR) {
                    if (assertionError == null)
                        assertionError = new UnexpectedStateChange(newInfo.playerStatus);
                } else if (newInfo.playerStatus == PlayerStatus.PLAYING) {
                    if (countDownLatch.getCount() == 0) {
                        if (assertionError == null)
                            assertionError = new UnexpectedStateChange(newInfo.playerStatus);
                    } else {
                        countDownLatch.countDown();
                    }
                }

            }

            @Override
            public boolean onMediaPlayerError(Object inObj, int what, int extra) {
                if (assertionError == null) {
                    assertionError = new AssertionFailedError("Unexpected call of onMediaPlayerError");
                }
                return false;
            }
        };
        PlaybackServiceMediaPlayer psmp = new LocalPSMP(c, callback);
        if (initialState == PlayerStatus.PREPARED || initialState == PlayerStatus.PLAYING || initialState == PlayerStatus.PAUSED) {
            boolean startWhenPrepared = (initialState != PlayerStatus.PREPARED);
            psmp.playMediaObject(writeTestPlayable(PLAYABLE_FILE_URL, PLAYABLE_LOCAL_URL), false, startWhenPrepared, true);
        }
        if (initialState == PlayerStatus.PAUSED) {
            psmp.pause(false, false);
        }
        psmp.resume();
        boolean res = countDownLatch.await(timeoutSeconds, TimeUnit.SECONDS);
        if (assertionError != null)
            throw assertionError;
        assertTrue(res || (initialState != PlayerStatus.PAUSED && initialState != PlayerStatus.PREPARED));
        psmp.shutdown();
    }

    public void testResumePausedState() throws InterruptedException {
        resumeTestSkeleton(PlayerStatus.PAUSED, LATCH_TIMEOUT_SECONDS);
    }

    public void testResumePreparedState() throws InterruptedException {
        resumeTestSkeleton(PlayerStatus.PREPARED, LATCH_TIMEOUT_SECONDS);
    }

    public void testResumePlayingState() throws InterruptedException {
        resumeTestSkeleton(PlayerStatus.PLAYING, 1);
    }

    private void prepareTestSkeleton(final PlayerStatus initialState, long timeoutSeconds) throws InterruptedException {
        final Context c = getInstrumentation().getTargetContext();
        final int latchCount = 1;
        final CountDownLatch countDownLatch = new CountDownLatch(latchCount);
        PlaybackServiceMediaPlayer.PSMPCallback callback = new DefaultPSMPCallback() {
            @Override
            public void statusChanged(LocalPSMP.PSMPInfo newInfo) {
                checkPSMPInfo(newInfo);
                if (newInfo.playerStatus == PlayerStatus.ERROR) {
                    if (assertionError == null)
                        assertionError = new UnexpectedStateChange(newInfo.playerStatus);
                } else {
                    if (initialState == PlayerStatus.INITIALIZED && newInfo.playerStatus == PlayerStatus.PREPARED) {
                        countDownLatch.countDown();
                    } else if (initialState != PlayerStatus.INITIALIZED && initialState == newInfo.playerStatus) {
                        countDownLatch.countDown();
                    }
                }
            }

            @Override
            public boolean onMediaPlayerError(Object inObj, int what, int extra) {
                if (assertionError == null)
                    assertionError = new AssertionFailedError("Unexpected call to onMediaPlayerError");
                return false;
            }
        };
        PlaybackServiceMediaPlayer psmp = new LocalPSMP(c, callback);
        Playable p = writeTestPlayable(PLAYABLE_FILE_URL, PLAYABLE_LOCAL_URL);
        if (initialState == PlayerStatus.INITIALIZED
                || initialState == PlayerStatus.PLAYING
                || initialState == PlayerStatus.PREPARED
                || initialState == PlayerStatus.PAUSED) {
            boolean prepareImmediately = (initialState != PlayerStatus.INITIALIZED);
            boolean startWhenPrepared = (initialState != PlayerStatus.PREPARED);
            psmp.playMediaObject(p, false, startWhenPrepared, prepareImmediately);
            if (initialState == PlayerStatus.PAUSED) {
                psmp.pause(false, false);
            }
            psmp.prepare();
        }

        boolean res = countDownLatch.await(timeoutSeconds, TimeUnit.SECONDS);
        if (initialState != PlayerStatus.INITIALIZED) {
            assertEquals(initialState, psmp.getPSMPInfo().playerStatus);
        }

        if (assertionError != null)
            throw assertionError;
        assertTrue(res);
        psmp.shutdown();
    }

    public void testPrepareInitializedState() throws InterruptedException {
        prepareTestSkeleton(PlayerStatus.INITIALIZED, LATCH_TIMEOUT_SECONDS);
    }

    public void testPreparePlayingState() throws InterruptedException {
        prepareTestSkeleton(PlayerStatus.PLAYING, 1);
    }

    public void testPreparePausedState() throws InterruptedException {
        prepareTestSkeleton(PlayerStatus.PAUSED, 1);
    }

    public void testPreparePreparedState() throws InterruptedException {
        prepareTestSkeleton(PlayerStatus.PREPARED, 1);
    }

    private void reinitTestSkeleton(final PlayerStatus initialState, final long timeoutSeconds) throws InterruptedException {
        final Context c = getInstrumentation().getTargetContext();
        final int latchCount = 2;
        final CountDownLatch countDownLatch = new CountDownLatch(latchCount);
        PlaybackServiceMediaPlayer.PSMPCallback callback = new DefaultPSMPCallback() {
            @Override
            public void statusChanged(LocalPSMP.PSMPInfo newInfo) {
                checkPSMPInfo(newInfo);
                if (newInfo.playerStatus == PlayerStatus.ERROR) {
                    if (assertionError == null)
                        assertionError = new UnexpectedStateChange(newInfo.playerStatus);
                } else {
                    if (newInfo.playerStatus == initialState) {
                        countDownLatch.countDown();
                    } else if (countDownLatch.getCount() < latchCount && newInfo.playerStatus == PlayerStatus.INITIALIZED) {
                        countDownLatch.countDown();
                    }
                }
            }

            @Override
            public boolean onMediaPlayerError(Object inObj, int what, int extra) {
                if (assertionError == null)
                    assertionError = new AssertionFailedError("Unexpected call to onMediaPlayerError");
                return false;
            }
        };
        PlaybackServiceMediaPlayer psmp = new LocalPSMP(c, callback);
        Playable p = writeTestPlayable(PLAYABLE_FILE_URL, PLAYABLE_LOCAL_URL);
        boolean prepareImmediately = initialState != PlayerStatus.INITIALIZED;
        boolean startImmediately = initialState != PlayerStatus.PREPARED;
        psmp.playMediaObject(p, false, startImmediately, prepareImmediately);
        if (initialState == PlayerStatus.PAUSED) {
            psmp.pause(false, false);
        }
        psmp.reinit();
        boolean res = countDownLatch.await(timeoutSeconds, TimeUnit.SECONDS);
        if (assertionError != null)
            throw assertionError;
        assertTrue(res);
        psmp.shutdown();
    }

    public void testReinitPlayingState() throws InterruptedException {
        reinitTestSkeleton(PlayerStatus.PLAYING, LATCH_TIMEOUT_SECONDS);
    }

    public void testReinitPausedState() throws InterruptedException {
        reinitTestSkeleton(PlayerStatus.PAUSED, LATCH_TIMEOUT_SECONDS);
    }

    public void testPreparedPlayingState() throws InterruptedException {
        reinitTestSkeleton(PlayerStatus.PREPARED, LATCH_TIMEOUT_SECONDS);
    }

    public void testReinitInitializedState() throws InterruptedException {
        reinitTestSkeleton(PlayerStatus.INITIALIZED, LATCH_TIMEOUT_SECONDS);
    }

    private static class UnexpectedStateChange extends AssertionFailedError {
        public UnexpectedStateChange(PlayerStatus status) {
            super("Unexpected state change: " + status);
        }
    }

    private class DefaultPSMPCallback implements PlaybackServiceMediaPlayer.PSMPCallback {
        @Override
        public void statusChanged(PlaybackServiceMediaPlayer.PSMPInfo newInfo) {

        }

        @Override
        public void shouldStop() {

        }

        @Override
        public void playbackSpeedChanged(float s) {

        }

        @Override
        public void setSpeedAbilityChanged() {

        }

        @Override
        public void onBufferingUpdate(int percent) {

        }

        @Override
        public void onMediaChanged(boolean reloadUI) {

        }

        @Override
        public boolean onMediaPlayerInfo(int code, @StringRes int resourceId) {
            return false;
        }

        @Override
        public boolean onMediaPlayerError(Object inObj, int what, int extra) {
            return false;
        }

        @Override
        public void onPostPlayback(@NonNull Playable media, boolean ended, boolean skipped, boolean playingNext) {

        }

        @Override
        public void onPlaybackStart(@NonNull Playable playable, int position) {

        }

        @Override
        public void onPlaybackPause(Playable playable, int position) {

        }

        @Override
        public Playable getNextInQueue(Playable currentMedia) {
            return null;
        }

        @Override
        public void onPlaybackEnded(MediaType mediaType, boolean stopPlaying) {

        }
    }
}
