package de.danoeh.antennapod.ui.episodeslist;

import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import org.greenrobot.eventbus.EventBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.event.FeedItemEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.FeedMedia;
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class EpisodesListFragmentStaleSnapshotTest {

    private final TestScheduler computationScheduler = new TestScheduler();
    private final TestScheduler mainScheduler = new TestScheduler();
    private TestEpisodesListFragment fragment;
    private EpisodeItemListAdapter listAdapter;

    @Before
    public void setUp() throws Exception {
        RxJavaPlugins.setComputationSchedulerHandler(scheduler -> computationScheduler);
        RxAndroidPlugins.setMainThreadSchedulerHandler(scheduler -> mainScheduler);

        FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
        listAdapter = new EpisodeItemListAdapter(activity);
        fragment = new TestEpisodesListFragment();
        fragment.listAdapter = listAdapter;
        Field progressBarField = EpisodesListFragment.class.getDeclaredField("progressBar");
        progressBarField.setAccessible(true);
        progressBarField.set(fragment, new ProgressBar(activity));
        EventBus.getDefault().register(fragment);
    }

    @After
    public void tearDown() {
        EventBus.getDefault().unregister(fragment);
        RxJavaPlugins.reset();
        RxAndroidPlugins.reset();
    }

    @Test
    public void feedItemEventUpdateSurvivesDeliveryOfStaleSnapshot() {
        FeedItem staleItem = createItem(false);
        assertFalse(staleItem.isDownloaded());
        fragment.snapshot = Collections.singletonList(staleItem);

        fragment.loadItems();
        computationScheduler.triggerActions();
        mainScheduler.triggerActions();
        assertSame(staleItem, listAdapter.getItem(0));

        fragment.loadItems();
        computationScheduler.triggerActions();

        FeedItem downloadedItem = createItem(true);
        assertTrue(downloadedItem.isDownloaded());
        EventBus.getDefault().post(new FeedItemEvent(Collections.singletonList(downloadedItem), false));
        assertSame(downloadedItem, fragment.episodes.get(0));

        mainScheduler.triggerActions();

        assertTrue(fragment.episodes.get(0).isDownloaded());
        assertSame(downloadedItem, fragment.episodes.get(0));
        assertSame(downloadedItem, listAdapter.getItem(0));
    }

    private FeedItem createItem(boolean downloaded) {
        FeedItem item = new FeedItem();
        item.setId(1);
        item.setTitle("Item 1");
        long downloadDate = downloaded ? System.currentTimeMillis() : 0;
        item.setMedia(new FeedMedia(0, item, 0, 0, 0, "audio/mpeg", null,
                "https://example.com/item1.mp3", downloadDate, null, 0, 0L));
        return item;
    }

    private static class TestEpisodesListFragment extends EpisodesListFragment {
        private List<FeedItem> snapshot = new ArrayList<>();

        @NonNull
        @Override
        protected List<FeedItem> loadData() {
            return new ArrayList<>(snapshot);
        }

        @NonNull
        @Override
        protected List<FeedItem> loadMoreData(int page) {
            return new ArrayList<>();
        }

        @Override
        protected int loadTotalItemCount() {
            return snapshot.size();
        }

        @NonNull
        @Override
        protected FeedItemFilter getFilter() {
            return FeedItemFilter.unfiltered();
        }

        @NonNull
        @Override
        protected String getFragmentTag() {
            return "EpisodesListFragmentStaleSnapshotTest";
        }
    }
}
