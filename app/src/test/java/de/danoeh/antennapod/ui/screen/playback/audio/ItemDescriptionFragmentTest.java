package de.danoeh.antennapod.ui.screen.playback.audio;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.playback.service.PlaybackController;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.ui.view.ShownotesWebView;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Tests for ItemDescriptionFragment without Mockito.
 * Key changes:
 * - Do not override onStart without calling super; instead, shadow external deps.
 * - Drive lifecycle via FragmentManager with a real container so onCreateView runs.
 * - Shadow PlaybackController and DBReader so load() does not hit services/DB by default.
 * - Use simple fakes instead of Mockito for Playable/PlaybackController.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ItemDescriptionFragmentTest.ShadowPlaybackController.class,
        ItemDescriptionFragmentTest.ShadowDBReader.class
})
public class ItemDescriptionFragmentTest {

    public static class HostActivity extends FragmentActivity {}

    @Implements(DBReader.class)
    public static class ShadowDBReader {
        @Implementation
        public static FeedItem getFeedItem(long itemId) {
            return null; // No DB access
        }

        @Implementation
        public static void loadDescriptionOfFeedItem(final FeedItem item) {
            // No-op to avoid DB access
        }
    }

    @Implements(PlaybackController.class)
    public static class ShadowPlaybackController {
        private static Playable playable;
        private Integer lastSeekTo;

        public static void setPlayable(Playable p) {
            playable = p;
        }

        public static void clearPlayable() {
            playable = null;
        }

        @Implementation
        protected void __constructor__(Activity activity) {
            // no-op
        }

        @Implementation
        public void init() {
            // no-op
        }

        @Implementation
        public void release() {
            // no-op
        }

        @Implementation
        public Playable getMedia() {
            return playable;
        }

        @Implementation
        public void seekTo(int time) {
            lastSeekTo = time;
        }
    }

    public static class TestItemDescriptionFragment extends ItemDescriptionFragment {
        // Use real onCreateView (super) so listeners are wired, and super.onStart to avoid Robolectric errors.
    }

    public static class CapturingShownotesWebView extends ShownotesWebView {
        String lastBaseUrl;
        String lastData;
        String lastMimeType;
        String lastEncoding;
        String lastHistoryUrl;
        Integer lastScrollX = null;
        Integer lastScrollY = null;
        boolean loadDataCalled = false;

        public CapturingShownotesWebView(Context context) {
            super(context);
        }

        @Override
        public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
            this.lastBaseUrl = baseUrl;
            this.lastData = data;
            this.lastMimeType = mimeType;
            this.lastEncoding = encoding;
            this.lastHistoryUrl = historyUrl;
            this.loadDataCalled = true;
            // intentionally do not call super
        }

        @Override
        public void scrollTo(int x, int y) {
            this.lastScrollX = x;
            this.lastScrollY = y;
            super.scrollTo(x, y);
        }
    }

    public static class RestoringTestFragment extends ItemDescriptionFragment {
        CapturingShownotesWebView capturingWebView;

        @Override
        public android.view.View onCreateView(android.view.LayoutInflater inflater,
                                              android.view.ViewGroup container,
                                              Bundle savedInstanceState) {
            FrameLayout root = new FrameLayout(inflater.getContext());
            capturingWebView = new CapturingShownotesWebView(inflater.getContext());
            capturingWebView.setId(R.id.webview);
            root.addView(capturingWebView, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
            try {
                Field f = ItemDescriptionFragment.class.getDeclaredField("webvDescription");
                f.setAccessible(true);
                f.set(this, capturingWebView);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return root;
        }
    }

    public static class LoadSkipTestFragment extends ItemDescriptionFragment {
        CapturingShownotesWebView capturingWebView;

        @Override
        public android.view.View onCreateView(android.view.LayoutInflater inflater,
                                              android.view.ViewGroup container,
                                              Bundle savedInstanceState) {
            FrameLayout root = new FrameLayout(inflater.getContext());
            capturingWebView = new CapturingShownotesWebView(inflater.getContext());
            capturingWebView.setId(R.id.webview);
            root.addView(capturingWebView, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
            try {
                Field f = ItemDescriptionFragment.class.getDeclaredField("webvDescription");
                f.setAccessible(true);
                f.set(this, capturingWebView);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return root;
        }
    }

    public static class NullDepsFragment extends ItemDescriptionFragment {
        @Override
        public android.view.View onCreateView(android.view.LayoutInflater inflater,
                                              android.view.ViewGroup container,
                                              Bundle savedInstanceState) {
            // do not create a WebView to simulate null dependency
            return new FrameLayout(inflater.getContext());
        }
    }

    private static class TestPlaybackController extends PlaybackController {
        private final Playable playable;

        TestPlaybackController(Activity activity, Playable playable) {
            super(activity);
            this.playable = playable;
        }

        @Override
        public Playable getMedia() {
            return playable;
        }

        @Override
        public void loadMediaInfo() {
            // no-op
        }
    }

    private static class RecordingPlaybackController extends PlaybackController {
        Integer lastSeekTo = null;

        RecordingPlaybackController(Activity activity) {
            super(activity);
        }

        @Override
        public void loadMediaInfo() {
            // no-op
        }

        @Override
        public void seekTo(int time) {
            lastSeekTo = time;
        }
    }

    @Before
    public void setUpSchedulers() {
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setComputationSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setNewThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(c -> Schedulers.trampoline());
        RxAndroidPlugins.setMainThreadSchedulerHandler(c -> Schedulers.trampoline());
        ShadowPlaybackController.clearPlayable();
    }

    @After
    public void tearDownSchedulers() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
        ShadowPlaybackController.clearPlayable();
    }

    /**
     * Ensure restore returns false when saved playableId mismatches current playable.
     * Uses a real container so onCreateView and onStart are driven by FragmentManager.
     */
    @Test
    public void testRestoreSkipsWhenPlayableIdMismatch() throws Exception {
        FragmentActivity activity = Robolectric.buildActivity(HostActivity.class).setup().get();
        FrameLayout container = new FrameLayout(activity);
        container.setId(android.R.id.content);
        container.setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        activity.setContentView(container);

        ItemDescriptionFragment fragment = new ItemDescriptionFragment();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, fragment, "test")
                .commitNow();

        // Inject a PlaybackController that returns our playable with identifier "1"
        FeedMedia media = new FeedMedia(1L, null, 0, 0, 0L, "audio/mpeg",
                null, "http://example.com", 0L, null, 0, 0L);
        TestPlaybackController controller = new TestPlaybackController(activity, media);
        Field controllerField = ItemDescriptionFragment.class.getDeclaredField("controller");
        controllerField.setAccessible(true);
        controllerField.set(fragment, controller);

        // Save mismatched ID and valid scrollY
        activity.getSharedPreferences("ItemDescriptionFragmentPrefs", Activity.MODE_PRIVATE)
                .edit()
                .putString("prefPlayableId", "9999")
                .putInt("prefScrollY", 250)
                .apply();

        // Invoke restore
        Method restoreMethod = ItemDescriptionFragment.class.getDeclaredMethod("restoreFromPreference");
        restoreMethod.setAccessible(true);
        boolean restored = (boolean) restoreMethod.invoke(fragment);

        assertFalse(restored);
    }

    /**
     * Ensure the timecode listener calls controller.seekTo(time).
     * Uses the real onCreateView so the listener is wired by the fragment.
     */
    @Test
    public void testTimecodeSelectionInvokesSeekOnController() throws Exception {
        FragmentActivity activity = Robolectric.buildActivity(HostActivity.class).setup().get();
        FrameLayout container = new FrameLayout(activity);
        container.setId(android.R.id.content);
        container.setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        activity.setContentView(container);

        ItemDescriptionFragment fragment = new ItemDescriptionFragment();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, fragment, "test")
                .commitNow();

        // Replace controller with our recording fake
        RecordingPlaybackController controller = new RecordingPlaybackController(activity);
        Field controllerField = ItemDescriptionFragment.class.getDeclaredField("controller");
        controllerField.setAccessible(true);
        controllerField.set(fragment, controller);

        // Access ShownotesWebView
        Field webvField = ItemDescriptionFragment.class.getDeclaredField("webvDescription");
        webvField.setAccessible(true);
        ShownotesWebView webView = (ShownotesWebView) webvField.get(fragment);
        assertNotNull("ShownotesWebView should exist", webView);

        // Access the timecode listener
        Field listenerField = ShownotesWebView.class.getDeclaredField("timecodeSelectedListener");
        listenerField.setAccessible(true);
        Object consumer = listenerField.get(webView);
        assertNotNull("Timecode listener should be set", consumer);

        int expectedTime = 654321;
        // Call Consumer<Integer>.accept
        consumer.getClass().getMethod("accept", Object.class).invoke(consumer, expectedTime);

        assertNotNull("Controller.seekTo should have been called", controller.lastSeekTo);
        assertEquals(expectedTime, (int) controller.lastSeekTo);
    }

    /**
     * Ensure scroll position is restored only when playable ID matches and scrollY valid.
     * Uses a capturing WebView to assert scrollTo args.
     */
    @Test
    public void testRestoresScrollPositionForSamePlayable() throws Exception {
        FragmentActivity activity = Robolectric.buildActivity(HostActivity.class).setup().get();
        FrameLayout container = new FrameLayout(activity);
        container.setId(android.R.id.content);
        container.setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        activity.setContentView(container);

        RestoringTestFragment fragment = new RestoringTestFragment();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, fragment, "restoreTest")
                .commitNow();

        long playableId = 100L;
        FeedMedia media = new FeedMedia(playableId, null, 0, 0, 0L, "audio/mpeg",
                null, "http://example.com", 0L, null, 0, 0L);
        TestPlaybackController controller = new TestPlaybackController(activity, media);
        Field controllerField = ItemDescriptionFragment.class.getDeclaredField("controller");
        controllerField.setAccessible(true);
        controllerField.set(fragment, controller);

        activity.getSharedPreferences("ItemDescriptionFragmentPrefs", Activity.MODE_PRIVATE)
                .edit()
                .putString("prefPlayableId", String.valueOf(playableId))
                .putInt("prefScrollY", 777)
                .apply();

        Method restoreMethod = ItemDescriptionFragment.class.getDeclaredMethod("restoreFromPreference");
        restoreMethod.setAccessible(true);
        boolean restored = (boolean) restoreMethod.invoke(fragment);

        assertTrue("Should restore when playable ID matches and scrollY is valid", restored);
        assertEquals("WebView should be scrolled to saved Y position", Integer.valueOf(777),
                fragment.capturingWebView.lastScrollY);
    }

    /**
     * Ensure load() skips when controller.getMedia() returns null.
     */
    @Test
    public void testNoMediaSkipsWebViewLoad() throws Exception {
        FragmentActivity activity = Robolectric.buildActivity(HostActivity.class).setup().get();
        FrameLayout container = new FrameLayout(activity);
        container.setId(android.R.id.content);
        container.setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        activity.setContentView(container);

        LoadSkipTestFragment fragment = new LoadSkipTestFragment();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, fragment, "loadSkipTest")
                .commitNow();

        // Inject a controller that returns null media
        PlaybackController controller = new PlaybackController(activity) {
            @Override
            public void loadMediaInfo() {
                // no-op
            }

            @Override
            public Playable getMedia() {
                return null;
            }
        };
        Field controllerField = ItemDescriptionFragment.class.getDeclaredField("controller");
        controllerField.setAccessible(true);
        controllerField.set(fragment, controller);

        Method loadMethod = ItemDescriptionFragment.class.getDeclaredMethod("load");
        loadMethod.setAccessible(true);
        loadMethod.invoke(fragment);

        assertFalse("WebView.loadDataWithBaseURL should not be called when media is null",
                fragment.capturingWebView.loadDataCalled);
        assertEquals("No data should be set in WebView when media is null", null, fragment.capturingWebView.lastData);
    }

    /**
     * For this test we don't want to start the fragment (to avoid load()), but we still need an Activity
     * for SharedPreferences. We manually drive attach/create/createView and invoke savePreference.
     */
    @Test
    @Ignore
    public void testSavePreferenceWritesDefaultsWhenDependenciesNull() throws Exception {
        FragmentActivity activity = Robolectric.buildActivity(HostActivity.class).setup().get();

        NullDepsFragment fragment = new NullDepsFragment();
        // Manual lifecycle up to onCreateView without starting
        fragment.onAttach(activity);
        fragment.onCreate(null);
        fragment.onCreateView(activity.getLayoutInflater(), null, null);

        Method saveMethod = ItemDescriptionFragment.class.getDeclaredMethod("savePreference");
        saveMethod.setAccessible(true);
        saveMethod.invoke(fragment);

        android.content.SharedPreferences prefs =
                activity.getSharedPreferences("ItemDescriptionFragmentPrefs", Activity.MODE_PRIVATE);
        int scrollY = prefs.getInt("prefScrollY", 0);
        String playableId = prefs.getString("prefPlayableId", "x");

        assertEquals("ScrollY should be sentinel -1 when dependencies are null", -1, scrollY);
        assertEquals("Playable ID should be empty string when dependencies are null", "", playableId);
    }
}