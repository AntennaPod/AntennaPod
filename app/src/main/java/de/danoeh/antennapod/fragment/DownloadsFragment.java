package de.danoeh.antennapod.fragment;

import android.content.res.Resources;
import android.os.Bundle;
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

    public static final String ARG_SELECTED_TAB = "selected_tab";

    public static final int POS_RUNNING = 0;
    public static final int POS_COMPLETED = 1;
    public static final int POS_LOG = 2;

    private ViewPager pager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.pager_fragment, container, false);
        pager = (ViewPager) root.findViewById(R.id.pager);
        DownloadsPagerAdapter pagerAdapter = new DownloadsPagerAdapter(getChildFragmentManager(), getResources());
        pager.setAdapter(pagerAdapter);
        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            int tab = getArguments().getInt(ARG_SELECTED_TAB);
            pager.setCurrentItem(tab, false);
        }
    }

    public class DownloadsPagerAdapter extends FragmentPagerAdapter {

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
