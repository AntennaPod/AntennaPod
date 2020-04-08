package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
public class ItemPagerFragment extends Fragment {
    private static final String ARG_FEEDITEMS = "feeditems";
    private static final String ARG_FEEDITEM_POS = "feeditem_pos";

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View layout = inflater.inflate(R.layout.feeditem_pager_fragment, container, false);
        Toolbar toolbar = layout.findViewById(R.id.toolbar);
        toolbar.setTitle("");
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        feedItems = getArguments().getLongArray(ARG_FEEDITEMS);
        int feedItemPos = getArguments().getInt(ARG_FEEDITEM_POS);

        ViewPager2 pager = layout.findViewById(R.id.pager);
        // FragmentStatePagerAdapter documentation:
        // > When using FragmentStatePagerAdapter the host ViewPager must have a valid ID set.
        // When opening multiple ItemPagerFragments by clicking "item" -> "visit podcast" -> "item" -> etc,
        // the ID is no longer unique and FragmentStatePagerAdapter does not display any pages.
        int newId = ViewCompat.generateViewId();
        pager.setId(newId);
        pager.setAdapter(new ItemPagerAdapter(this));
        pager.setCurrentItem(feedItemPos);
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
                    getActivity().invalidateOptionsMenu();
                }, Throwable::printStackTrace);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isAdded() || item == null) {
            return;
        }
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.feeditem_options, menu);
        if (item.hasMedia()) {
            FeedItemMenuHandler.onPrepareMenu(menu, item);
        } else {
            // these are already available via button1 and button2
            FeedItemMenuHandler.onPrepareMenu(menu, item,
                    R.id.mark_read_item, R.id.visit_website_item);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.open_podcast:
                openPodcast();
                return true;
            default:
                return FeedItemMenuHandler.onMenuItemClicked(this, menuItem.getItemId(), item);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) {
        for (FeedItem item : event.items) {
            if (this.item != null && this.item.getId() == item.getId()) {
                this.item = item;
                getActivity().invalidateOptionsMenu();
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
