package de.danoeh.antennapod.fragment.preferences;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.fragment.PagedToolbarFragment;

/**
 * Displays the 'statistics' screen
 */
public class StatisticsFragment extends PagedToolbarFragment {

    public static final String TAG = "StatisticsFragment";

    private static final int POS_SUBSCRIPTIONS = 0;
    private static final int POS_YEARS = 1;
    private static final int POS_SPACE_TAKEN = 2;
    private static final int TOTAL_COUNT = 3;

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private Toolbar toolbar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);

        View rootView = inflater.inflate(R.layout.pager_fragment, container, false);
        viewPager = rootView.findViewById(R.id.viewpager);
        toolbar = rootView.findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.statistics_label));
        toolbar.inflateMenu(R.menu.statistics);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
        viewPager.setAdapter(new StatisticsPagerAdapter(this));
        // Give the TabLayout the ViewPager
        tabLayout = rootView.findViewById(R.id.sliding_tabs);
        super.setupPagedToolbar(toolbar, viewPager);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case POS_SUBSCRIPTIONS:
                    tab.setText(R.string.subscriptions_label);
                    break;
                case POS_YEARS:
                    tab.setText(R.string.years_statistics_label);
                    break;
                case POS_SPACE_TAKEN:
                    tab.setText(R.string.downloads_label);
                    break;
                default:
                    break;
            }
        }).attach();
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getActivity().getClass() == PreferenceActivity.class) {
            ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.statistics_label);
        }
    }

    public static class StatisticsPagerAdapter extends FragmentStateAdapter {

        StatisticsPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case POS_SUBSCRIPTIONS:
                    return new SubscriptionStatisticsFragment();
                case POS_YEARS:
                    return new YearsStatisticsFragment();
                default:
                case POS_SPACE_TAKEN:
                    return new DownloadStatisticsFragment();
            }
        }

        @Override
        public int getItemCount() {
            return TOTAL_COUNT;
        }
    }
}
