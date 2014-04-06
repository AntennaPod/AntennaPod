package de.danoeh.antennapod.fragment;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;

/**
 * Shows the CompletedDownloadsFragment and the RunningDownloadsFragment
 */
public class DownloadsFragment extends Fragment {


    private ViewPager pager;
    private MainActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.downloads_fragment, container, false);
        pager = (ViewPager) root.findViewById(R.id.pager);
        DownloadsPagerAdapter pagerAdapter = new DownloadsPagerAdapter(getChildFragmentManager(), getResources());
        pager.setAdapter(pagerAdapter);
        final ActionBar actionBar = activity.getMainActivtyActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            @Override
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                pager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

            }

            @Override
            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

            }
        };
        actionBar.removeAllTabs();
        actionBar.addTab(actionBar.newTab()
                .setText(R.string.downloads_running_label)
                .setTabListener(tabListener));
        actionBar.addTab(actionBar.newTab()
                .setText(R.string.downloads_completed_label)
                .setTabListener(tabListener));
        actionBar.addTab(actionBar.newTab()
                .setText(R.string.downloads_log_label)
                .setTabListener(tabListener));

        pager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                actionBar.setSelectedNavigationItem(position);
            }
        });
        return root;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (MainActivity) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        activity.getMainActivtyActionBar().removeAllTabs();
        activity.getMainActivtyActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    }

    public class DownloadsPagerAdapter extends FragmentPagerAdapter {

        private final int POS_RUNNING = 0;
        private final int POS_COMPLETED = 1;
        private final int POS_LOG = 2;


        Resources resources;

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
