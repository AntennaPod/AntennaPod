package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import de.danoeh.antennapod.R;

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
    private int feedItemPos;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        feedItems = getArguments().getLongArray(ARG_FEEDITEMS);
        feedItemPos = getArguments().getInt(ARG_FEEDITEM_POS);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View layout = inflater.inflate(R.layout.feeditem_pager_fragment, container, false);

        ViewPager pager = layout.findViewById(R.id.pager);
        pager.setAdapter(new ItemPagerAdapter());
        pager.setCurrentItem(feedItemPos);

        return layout;
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
