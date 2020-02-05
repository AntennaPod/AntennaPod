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
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.CastEnabledActivity;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.event.FeedItemEvent;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.Flavors;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Displays information about a list of FeedItems.
 */
public class ItemPagerFragment extends Fragment {
    private static final String ARG_FEEDITEMS = "feeditems";
    private static final String ARG_FEEDITEM_POS = "feeditem_pos";

    /**
     * Creates a new instance of an ItemPagerFragment.
     *
     * @param feeditem The ID of the FeedItem that should be displayed.
     * @return The ItemFragment instance
     */
    public static ItemPagerFragment newInstance(long feeditem) {
        return newInstance(new long[] { feeditem }, 0);
    }

    /**
     * Creates a new instance of an ItemPagerFragment.
     *
     * @param feeditems The IDs of the FeedItems that belong to the same list
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

        feedItems = getArguments().getLongArray(ARG_FEEDITEMS);
        int feedItemPos = getArguments().getInt(ARG_FEEDITEM_POS);

        ViewPager pager = layout.findViewById(R.id.pager);
        // FragmentStatePagerAdapter documentation:
        // > When using FragmentStatePagerAdapter the host ViewPager must have a valid ID set.
        // When opening multiple ItemPagerFragments by clicking "item" -> "visit podcast" -> "item" -> etc,
        // the ID is no longer unique and FragmentStatePagerAdapter does not display any pages.
        int newId = ViewCompat.generateViewId();
        pager.setId(newId);
        pager.setAdapter(new ItemPagerAdapter());
        pager.setCurrentItem(feedItemPos);
        loadItem(feedItems[feedItemPos]);
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                loadItem(feedItems[position]);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

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
        if (Flavors.FLAVOR == Flavors.PLAY) {
            ((CastEnabledActivity) getActivity()).requestCastButton(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        inflater.inflate(R.menu.feeditem_options, menu);

        if (menu != null && item != null) {
            if (item.hasMedia()) {
                FeedItemMenuHandler.onPrepareMenu(menu, item);
            } else {
                // these are already available via button1 and button2
                FeedItemMenuHandler.onPrepareMenu(menu, item,
                        R.id.mark_read_item, R.id.visit_website_item);
            }
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

    private class ItemPagerAdapter extends FragmentStatePagerAdapter {

        ItemPagerAdapter() {
            super(getFragmentManager(), BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return ItemFragment.newInstance(feedItems[position]);
        }

        @Override
        public int getCount() {
            return feedItems.length;
        }
    }
}
