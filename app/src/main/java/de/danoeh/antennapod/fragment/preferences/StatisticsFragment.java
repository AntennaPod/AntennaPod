package de.danoeh.antennapod.fragment.preferences;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;

/**
 * Displays the 'statistics' screen
 */
public class StatisticsFragment extends Fragment {

    public static final String TAG = "StatisticsFragment";

    private static final int POS_LISTENED_HOURS = 0;
    private static final int POS_SPACE_TAKEN = 1;
    private static final int TOTAL_COUNT = 2;


    private TabLayout tabLayout;
    private ViewPager viewPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);

        View rootView = inflater.inflate(R.layout.pager_fragment, container, false);
        viewPager = rootView.findViewById(R.id.viewpager);
        viewPager.setAdapter(new StatisticsPagerAdapter(getChildFragmentManager(), getResources()));

        // Give the TabLayout the ViewPager
        tabLayout = rootView.findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.statistics_label);
    }

    public static class StatisticsPagerAdapter extends FragmentPagerAdapter {

        private final Resources resources;

        public StatisticsPagerAdapter(FragmentManager fm, Resources resources) {
            super(fm);
            this.resources = resources;
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return new PlaybackStatisticsFragment();
            } else {
                return new DownloadStatisticsFragment();
            }
        }

        @Override
        public int getCount() {
            return TOTAL_COUNT;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case POS_LISTENED_HOURS:
                    return resources.getString(R.string.playback_statistics_label);
                case POS_SPACE_TAKEN:
                    return resources.getString(R.string.download_statistics_label);
                default:
                    return super.getPageTitle(position);
            }
        }
    }
}
