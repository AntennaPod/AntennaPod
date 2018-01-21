package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.danoeh.antennapod.R;

/**
 * Shows the CompletedDownloadsFragment and the RunningDownloadsFragment
 */
public class DownloadsFragment extends Fragment {

    public static final String TAG = "DownloadsFragment";

    public static final String ARG_SELECTED_TAB = "selected_tab";

    public static final int POS_RUNNING = 0;
    private static final int POS_COMPLETED = 1;
    public static final int POS_LOG = 2;

    private static final String PREF_LAST_TAB_POSITION = "tab_position";

    private ViewPager viewPager;
    private TabLayout tabLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.pager_fragment, container, false);

        viewPager = (ViewPager)root.findViewById(R.id.viewpager);
        DownloadsPagerAdapter pagerAdapter = new DownloadsPagerAdapter(getChildFragmentManager(), getResources());
        viewPager.setAdapter(pagerAdapter);

        // Give the TabLayout the ViewPager
        tabLayout = (TabLayout) root.findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);

        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            int tab = getArguments().getInt(ARG_SELECTED_TAB);
            viewPager.setCurrentItem(tab, false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // save our tab selection
        SharedPreferences prefs = getActivity().getSharedPreferences(TAG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_LAST_TAB_POSITION, tabLayout.getSelectedTabPosition());
        editor.apply();
    }

    @Override
    public void onStart() {
        super.onStart();

        // restore our last position
        SharedPreferences prefs = getActivity().getSharedPreferences(TAG, Context.MODE_PRIVATE);
        int lastPosition = prefs.getInt(PREF_LAST_TAB_POSITION, 0);
        viewPager.setCurrentItem(lastPosition);
    }

    public static class DownloadsPagerAdapter extends FragmentPagerAdapter {

        final Resources resources;

        public DownloadsPagerAdapter(FragmentManager fm, Resources resources) {
            super(fm);
            this.resources = resources;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case POS_RUNNING:
                    return new RunningDownloadsFragment();
                case POS_COMPLETED:
                    return new CompletedDownloadsFragment();
                case POS_LOG:
                    return new DownloadLogFragment();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case POS_RUNNING:
                    return resources.getString(R.string.downloads_running_label);
                case POS_COMPLETED:
                    return resources.getString(R.string.downloads_completed_label);
                case POS_LOG:
                    return resources.getString(R.string.downloads_log_label);
                default:
                    return super.getPageTitle(position);
            }
        }
    }
}
