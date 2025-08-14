package de.danoeh.antennapod.ui.screen.playback.audio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

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

@RunWith(RobolectricTestRunner.class)
public class ItemDescriptionFragmentTest {

    public static class HostActivity extends FragmentActivity {
    }

    public static class TestItemDescriptionFragment extends ItemDescriptionFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, android.os.Bundle savedInstanceState) {
            // Avoid inflating original layout to skip ShownotesWebView construction
            return new FrameLayout(inflater.getContext());
        }

        @Override
        public void onStart() {
            // Prevent creating real PlaybackController and background work
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

    @Test
    public void testRestoreSkipsWhenPlayableIdMismatch() throws Exception {
        HostActivity activity = Robolectric.buildActivity(HostActivity.class).setup().get();
        TestItemDescriptionFragment fragment = new TestItemDescriptionFragment();
        activity.getSupportFragmentManager().beginTransaction().add(fragment, "test").commitNow();

        // Prepare a playable with identifier "1"
        FeedMedia media = new FeedMedia(1L, null, 0, 0, 0L, "audio/mpeg",
                null, "http://example.com", 0L, null, 0, 0L);

        // Inject a PlaybackController that returns our playable
        TestPlaybackController controller = new TestPlaybackController(activity, media);
        Field controllerField = ItemDescriptionFragment.class.getDeclaredField("controller");
        controllerField.setAccessible(true);
        controllerField.set(fragment, controller);

        // Set SharedPreferences with mismatched playable ID and valid scrollY
        activity.getSharedPreferences("ItemDescriptionFragmentPrefs", Activity.MODE_PRIVATE)
                .edit()
                .putString("prefPlayableId", "9999")
                .putInt("prefScrollY", 250)
                .apply();

        // Invoke restoreFromPreference via reflection
        Method restoreMethod = ItemDescriptionFragment.class.getDeclaredMethod("restoreFromPreference");
        restoreMethod.setAccessible(true);
        boolean restored = (boolean) restoreMethod.invoke(fragment);

        // Should return false due to playable ID mismatch
        assertFalse(restored);
    }

    @Test
    public void testTimecodeSelectionInvokesSeekOnController() throws Exception {
        HostActivity activity = Robolectric.buildActivity(HostActivity.class).setup().get();
        TestItemDescriptionFragment fragment = new TestItemDescriptionFragment();
        activity.getSupportFragmentManager().beginTransaction().add(fragment, "test").commitNow();

        // Inject a RecordingPlaybackController
        RecordingPlaybackController controller = new RecordingPlaybackController(activity);
        Field controllerField = ItemDescriptionFragment.class.getDeclaredField("controller");
        controllerField.setAccessible(true);
        controllerField.set(fragment, controller);

        // Access ShownotesWebView field
        Field webvField = ItemDescriptionFragment.class.getDeclaredField("webvDescription");
        webvField.setAccessible(true);
        ShownotesWebView webView = (ShownotesWebView) webvField.get(fragment);
        assertNotNull("ShownotesWebView should be initialized by onCreateView", webView);

        // Access and invoke the timecode listener
        Field listenerField = ShownotesWebView.class.getDeclaredField("timecodeSelectedListener");
        listenerField.setAccessible(true);
        Object consumer = listenerField.get(webView);
        assertNotNull("Timecode listener should be set on ShownotesWebView", consumer);

        // Invoke the Consumer<Integer>.accept(time)
        int expectedTime = 654321;
        consumer.getClass().getMethod("accept", Object.class).invoke(consumer, expectedTime);

        assertNotNull("Controller.seekTo should have been called", controller.lastSeekTo);
        assertEquals("Controller.seekTo should be called with the selected timecode",
                expectedTime, (int) controller.lastSeekTo);
    }

    public static class CapturingShownotesWebView extends ShownotesWebView {
        String lastBaseUrl;
        String lastData;
        String lastMimeType;
        String lastEncoding;
        String lastHistoryUrl;
        boolean loadDataCalled = false;
        Integer lastScrollX = null;
        Integer lastScrollY = null;

        public CapturingShownotesWebView(android.content.Context context) {
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
            // Do not call super to keep it deterministic in tests
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
        public android.view.View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

        @Override
        public void onStart() {
            // Prevent default controller creation and background work
        }
    }

    public static class LoadSkipTestFragment extends ItemDescriptionFragment {
        CapturingShownotesWebView capturingWebView;

        @Override
        public android.view.View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

        @Override
        public void onStart() {
            // Prevent default controller creation and background work
        }
    }

    public static class NullDepsFragment extends ItemDescriptionFragment {
        @Override
        public android.view.View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            // Do not create a WebView to simulate null dependency
            return new FrameLayout(inflater.getContext());
        }

        @Override
        public void onStart() {
            // Prevent default controller creation and background work
        }
    }

    private static class NullMediaController extends PlaybackController {
        NullMediaController(Activity activity) {
            super(activity);
        }

        @Override
        public Playable getMedia() {
            return null;
        }

        @Override
        public void loadMediaInfo() {
            // no-op
        }
    }

    @Test
    public void testRestoresScrollPositionForSamePlayable() throws Exception {
        HostActivity activity = Robolectric.buildActivity(HostActivity.class).setup().get();
        RestoringTestFragment fragment = new RestoringTestFragment();
        activity.getSupportFragmentManager().beginTransaction().add(fragment, "restoreTest").commitNow();

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

    @Test
    public void testNoMediaSkipsWebViewLoad() throws Exception {
        HostActivity activity = Robolectric.buildActivity(HostActivity.class).setup().get();
        LoadSkipTestFragment fragment = new LoadSkipTestFragment();
        activity.getSupportFragmentManager().beginTransaction().add(fragment, "loadSkipTest").commitNow();

        NullMediaController controller = new NullMediaController(activity);
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

    @Test
    public void testSavePreferenceWritesDefaultsWhenDependenciesNull() throws Exception {
        HostActivity activity = Robolectric.buildActivity(HostActivity.class).setup().get();
        NullDepsFragment fragment = new NullDepsFragment();
        activity.getSupportFragmentManager().beginTransaction().add(fragment, "nullDepsTest").commitNow();

        Method saveMethod = ItemDescriptionFragment.class.getDeclaredMethod("savePreference");
        saveMethod.setAccessible(true);
        saveMethod.invoke(fragment);

        android.content.SharedPreferences prefs = activity.getSharedPreferences("ItemDescriptionFragmentPrefs", Activity.MODE_PRIVATE);
        int scrollY = prefs.getInt("prefScrollY", 0);
        String playableId = prefs.getString("prefPlayableId", "x");

        assertEquals("ScrollY should be sentinel -1 when dependencies are null", -1, scrollY);
        assertEquals("Playable ID should be empty string when dependencies are null", "", playableId);
    }
}

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ItemDescriptionFragmentLoadShownotesTest.ShadowDBReader.class})
class ItemDescriptionFragmentLoadShownotesTest {

    public static class HostActivity extends FragmentActivity {
    }

    @Implements(DBReader.class)
    public static class ShadowDBReader {
        @Implementation
        public static FeedItem getFeedItem(long itemId) {
            return null; // No DB access in tests
        }

        @Implementation
        public static void loadDescriptionOfFeedItem(final FeedItem item) {
            // No-op to avoid DB access
        }
    }

    public static class CapturingShownotesWebView extends ShownotesWebView {
        String lastBaseUrl;
        String lastData;
        String lastMimeType;
        String lastEncoding;
        String lastHistoryUrl;

        public CapturingShownotesWebView(android.content.Context context) {
            super(context);
        }

        @Override
        public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
            this.lastBaseUrl = baseUrl;
            this.lastData = data;
            this.lastMimeType = mimeType;
            this.lastEncoding = encoding;
            this.lastHistoryUrl = historyUrl;
            // Do not call super to keep it simple and deterministic
        }
    }

    public static class LoadTestFragment extends ItemDescriptionFragment {
        CapturingShownotesWebView capturingWebView;

        @Override
        public android.view.View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

        @Override
        public void onStart() {
            // Prevent default controller creation and background work
        }
    }

    private static class TestPlaybackController extends PlaybackController {
        private final FeedMedia playable;

        TestPlaybackController(Activity activity, FeedMedia playable) {
            super(activity);
            this.playable = playable;
        }

        @Override
        public FeedMedia getMedia() {
            return playable;
        }

        @Override
        public void loadMediaInfo() {
            // no-op
        }
    }

    private static class TestFeedMedia extends FeedMedia {
        private final String description;

        TestFeedMedia(long id, String description, int durationMs) {
            super(id, null, durationMs, 0, 0L, "audio/mpeg",
                    null, "http://example.com", 0L, null, 0, 0L);
            this.description = description;
            setDuration(durationMs);
        }

        @Override
        public String getDescription() {
            return description;
        }
    }

    @Before
    public void setUpSchedulers() {
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
        RxJavaPlugins.setComputationSchedulerHandler(scheduler -> Schedulers.trampoline());
        RxJavaPlugins.setNewThreadSchedulerHandler(scheduler -> Schedulers.trampoline());
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(schedulerCallable -> Schedulers.trampoline());
    }

    @After
    public void tearDownSchedulers() {
        RxJavaPlugins.reset();
        RxAndroidPlugins.reset();
    }

    @Test
    public void testLoadsAndDisplaysCleanedShownotesFromFeedMedia() throws Exception {
        HostActivity activity = Robolectric.buildActivity(HostActivity.class).setup().get();
        LoadTestFragment fragment = new LoadTestFragment();
        activity.getSupportFragmentManager().beginTransaction().add(fragment, "loadTest").commitNow();

        // Prepare FeedMedia with raw shownotes containing a CSS color property and a short timecode
        String rawShownotes = "<div style=\"color: red; font-weight:bold;\">Hello<br>See 01:02 for info</div>";
        int durationMs = 10 * 60 * 1000; // 10 minutes to ensure short timecode is treated as MM:SS
        TestFeedMedia media = new TestFeedMedia(42L, rawShownotes, durationMs);

        // Inject controller returning our FeedMedia
        TestPlaybackController controller = new TestPlaybackController(activity, media);
        Field controllerField = ItemDescriptionFragment.class.getDeclaredField("controller");
        controllerField.setAccessible(true);
        controllerField.set(fragment, controller);

        // Invoke private load() to trigger processing
        Method loadMethod = ItemDescriptionFragment.class.getDeclaredMethod("load");
        loadMethod.setAccessible(true);
        loadMethod.invoke(fragment);

        // Verify that HTML was loaded into the WebView and that it has been cleaned and timecodes linked
        CapturingShownotesWebView webView = fragment.capturingWebView;
        assertNotNull("WebView should have loaded data", webView.lastData);
        assertTrue("Base URL should be set", "https://127.0.0.1".equals(webView.lastBaseUrl));
        assertTrue("MIME type should be text/html", "text/html".equals(webView.lastMimeType));
        assertTrue("Encoding should be utf-8", "utf-8".equals(webView.lastEncoding));

        // CSS color property should be removed
        assertFalse("CSS color property should be stripped", webView.lastData.contains("color:"));

        // Timecode 01:02 should be converted to a clickable link with 62000 ms
        assertTrue("Timecode should be converted into a clickable link",
                webView.lastData.contains("antennapod://timecode/62000"));
    }
}
