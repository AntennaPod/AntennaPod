package de.danoeh.antennapod.fragment.preferences.about;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;

/**
 * Displays the 'about->Contributors' pager screen.
 */
public class ContributorsPagerFragment extends Fragment {

    public static final String TAG = "StatisticsFragment";

    private static final int POS_DEVELOPERS = 0;
    private static final int POS_TRANSLATORS = 1;
    private static final int POS_SPECIAL_THANKS = 2;
    private static final int TOTAL_COUNT = 3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);

        View rootView = inflater.inflate(R.layout.pager_fragment, container, false);
        ViewPager2 viewPager = rootView.findViewById(R.id.viewpager);
        viewPager.setAdapter(new StatisticsPagerAdapter(this));
        // Give the TabLayout the ViewPager
        TabLayout tabLayout = rootView.findViewById(R.id.sliding_tabs);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case POS_DEVELOPERS:
                    tab.setText(R.string.developers);
                    break;
                case POS_TRANSLATORS:
                    tab.setText(R.string.translators);
                    break;
                case POS_SPECIAL_THANKS:
                    tab.setText(R.string.special_thanks);
                    break;
                default:
                    break;
            }
        }).attach();

        rootView.findViewById(R.id.toolbar).setVisibility(View.GONE);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.contributors);
    }

    public static class StatisticsPagerAdapter extends FragmentStateAdapter {

        StatisticsPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case POS_TRANSLATORS:
                    return new TranslatorsFragment();
                case POS_SPECIAL_THANKS:
                    return new SpecialThanksFragment();
                default:
                case POS_DEVELOPERS:
                    return new DevelopersFragment();
            }
        }

        @Override
        public int getItemCount() {
            return TOTAL_COUNT;
        }
    }
}
