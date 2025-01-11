package de.danoeh.antennapod.ui.screen.episode;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.MaterialToolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.ui.appstartintent.OnlineFeedviewActivityStarter;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.event.FeedItemEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.ui.episodeslist.FeedItemMenuHandler;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.util.Collections;
import java.util.List;

/**
 * Displays information about a list of FeedItems.
 */
public class ItemPagerFragment extends Fragment implements MaterialToolbar.OnMenuItemClickListener {
    private static final String ARG_FEEDITEMS = "feeditems";
    private static final String ARG_FEEDITEM_POS = "feeditem_pos";
    private static final String KEY_PAGER_ID = "pager_id";
    private ViewPager2 pager;

    /**
     * Creates a new instance of an ItemPagerFragment.
     *
     * @return The ItemFragment instance
     */
    public static ItemPagerFragment newInstance(List<FeedItem> allItems, FeedItem currentItem) {
        int position = 0;
        long[] ids = new long[allItems.size()];
        for (int i = 0; i < allItems.size(); i++) {
            ids[i] = allItems.get(i).getId();
            if (ids[i] == currentItem.getId()) {
                position = i;
            }
        }
        ItemPagerFragment fragment = new ItemPagerFragment();
        Bundle args = new Bundle();
        args.putLongArray(ARG_FEEDITEMS, ids);
        args.putInt(ARG_FEEDITEM_POS, position);
        fragment.setArguments(args);
        return fragment;
    }

    private long[] feedItems;
    private FeedItem item;
    private Disposable disposable;
    private MaterialToolbar toolbar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View layout = inflater.inflate(R.layout.feeditem_pager_fragment, container, false);
        toolbar = layout.findViewById(R.id.toolbar);
        toolbar.setTitle("");
        toolbar.inflateMenu(R.menu.feeditem_options);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
        toolbar.setOnMenuItemClickListener(this);

        feedItems = getArguments().getLongArray(ARG_FEEDITEMS);
        final int feedItemPos = Math.max(0, getArguments().getInt(ARG_FEEDITEM_POS));

        pager = layout.findViewById(R.id.pager);
        // FragmentStatePagerAdapter documentation:
        // > When using FragmentStatePagerAdapter the host ViewPager must have a valid ID set.
        // When opening multiple ItemPagerFragments by clicking "item" -> "visit podcast" -> "item" -> etc,
        // the ID is no longer unique and FragmentStatePagerAdapter does not display any pages.
        int newId = View.generateViewId();
        if (savedInstanceState != null && savedInstanceState.getInt(KEY_PAGER_ID, 0) != 0) {
            // Restore state by using the same ID as before. ID collisions are prevented in MainActivity.
            newId = savedInstanceState.getInt(KEY_PAGER_ID, 0);
        }
        pager.setId(newId);
        pager.setAdapter(new ItemPagerAdapter(this));
        pager.setCurrentItem(feedItemPos, false);
        pager.setOffscreenPageLimit(1);
        loadItem(feedItems[feedItemPos]);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                loadItem(feedItems[position]);
            }
        });

        EventBus.getDefault().register(this);
        return layout;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_PAGER_ID, pager.getId());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private void loadItem(long itemId) {
        if (disposable != null) {
            disposable.dispose();
        }

        disposable = Observable.fromCallable(() -> DBReader.getFeedItem(itemId))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    item = result;
                    refreshToolbarState();
                }, Throwable::printStackTrace);
    }

    public void refreshToolbarState() {
        if (item == null) {
            return;
        }
        if (item.hasMedia()) {
            FeedItemMenuHandler.onPrepareMenu(toolbar.getMenu(), Collections.singletonList(item));
        } else {
            // these are already available via button1 and button2
            FeedItemMenuHandler.onPrepareMenu(toolbar.getMenu(), Collections.singletonList(item),
                    R.id.mark_read_item, R.id.visit_website_item);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.open_podcast) {
            openPodcast();
            return true;
        }
        return FeedItemMenuHandler.onMenuItemClicked(this, menuItem.getItemId(), item);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) {
        for (FeedItem item : event.items) {
            if (this.item != null && this.item.getId() == item.getId()) {
                this.item = item;
                refreshToolbarState();
                return;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(UnreadItemsUpdateEvent event) {
        refreshToolbarState();
    }

    private void openPodcast() {
        if (item == null) {
            return;
        }
        if (item.getFeed().getState() == Feed.STATE_SUBSCRIBED) {
            Intent intent = MainActivity.getIntentToOpenFeed(getContext(), item.getFeedId());
            startActivity(intent);
        } else {
            startActivity(new OnlineFeedviewActivityStarter(getContext(), item.getFeed().getDownloadUrl()).getIntent());
        }
    }

    private class ItemPagerAdapter extends FragmentStateAdapter {

        ItemPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return ItemFragment.newInstance(feedItems[position]);
        }

        @Override
        public int getItemCount() {
            return feedItems.length;
        }
    }
}
