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

/**
 * Displays the 'statistics' screen
 */
public class StatisticsFragment extends Fragment {

    public static final String TAG = "StatisticsFragment";

    private static final int POS_LISTENED_HOURS = 0;
    private static final int POS_SPACE_TAKEN = 1;
    private static final int TOTAL_COUNT = 2;


    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private Toolbar toolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);

        View rootView = inflater.inflate(R.layout.pager_fragment, container, false);
        viewPager = rootView.findViewById(R.id.viewpager);
        toolbar = rootView.findViewById(R.id.toolbar);
        viewPager.setAdapter(new StatisticsPagerAdapter(this));
        // Give the TabLayout the ViewPager
        tabLayout = rootView.findViewById(R.id.sliding_tabs);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case POS_LISTENED_HOURS:
                    tab.setText(R.string.playback_statistics_label);
                    break;
                case POS_SPACE_TAKEN:
                    tab.setText(R.string.download_statistics_label);
                    break;
                default:
                    break;
            }
        }).attach();

        if (getActivity().getClass() == PreferenceActivity.class) {
            rootView.findViewById(R.id.toolbar).setVisibility(View.GONE);
        } else {
            toolbar.setTitle(getString(R.string.statistics_label));
            toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

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
                case POS_LISTENED_HOURS:
                    return new PlaybackStatisticsFragment();
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
