package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.event.FeedItemEvent;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Displays information about a list of FeedItems.
 */
public class ItemPagerFragment extends Fragment implements Toolbar.OnMenuItemClickListener {
    private static final String ARG_FEEDITEMS = "feeditems";
    private static final String ARG_FEEDITEM_POS = "feeditem_pos";
    private static final String KEY_PAGER_ID = "pager_id";
    private ViewPager2 pager;

    /**
     * Creates a new instance of an ItemPagerFragment.
     *
     * @param feeditems   The IDs of the FeedItems that belong to the same list
     * @param feedItemPos The position of the FeedItem that is currently shown
     * @return The ItemFragment instance
     */
    public static ItemPagerFragment newInstance(long[] feeditems, int feedItemPos) {
        ItemPagerFragment fragment = new ItemPagerFragment();
        Bundle args = new Bundle();
        args.putLongArray(ARG_FEEDITEMS, feeditems);
        args.putInt(ARG_FEEDITEM_POS, feedItemPos);
        fragment.setArguments(args);
        return fragment;
    }

    private long[] feedItems;
    private FeedItem item;
    private Disposable disposable;
    private Toolbar toolbar;

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
        int feedItemPos = getArguments().getInt(ARG_FEEDITEM_POS);

        pager = layout.findViewById(R.id.pager);
        // FragmentStatePagerAdapter documentation:
        // > When using FragmentStatePagerAdapter the host ViewPager must have a valid ID set.
        // When opening multiple ItemPagerFragments by clicking "item" -> "visit podcast" -> "item" -> etc,
        // the ID is no longer unique and FragmentStatePagerAdapter does not display any pages.
        int newId = ViewCompat.generateViewId();
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
            FeedItemMenuHandler.onPrepareMenu(toolbar.getMenu(), item);
        } else {
            // these are already available via button1 and button2
            FeedItemMenuHandler.onPrepareMenu(toolbar.getMenu(), item,
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

    private void openPodcast() {
        Fragment fragment = FeedItemlistFragment.newInstance(item.getFeedId());
        ((MainActivity) getActivity()).loadChildFragment(fragment);
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
