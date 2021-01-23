package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;

/**
 * Shows the CompletedDownloadsFragment and the RunningDownloadsFragment.
 */
public class DownloadsFragment extends PagedToolbarFragment {

    public static final String TAG = "DownloadsFragment";

    public static final String ARG_SELECTED_TAB = "selected_tab";

    public static final int POS_RUNNING = 0;
    private static final int POS_COMPLETED = 1;
    public static final int POS_LOG = 2;
    private static final int TOTAL_COUNT = 3;

    private static final String PREF_LAST_TAB_POSITION = "tab_position";

    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.pager_fragment, container, false);
        Toolbar toolbar = root.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.downloads_label);
        toolbar.inflateMenu(R.menu.downloads);
        ((MainActivity) getActivity()).setupToolbarToggle(toolbar);

        viewPager = root.findViewById(R.id.viewpager);
        viewPager.setAdapter(new DownloadsPagerAdapter(this));
        viewPager.setOffscreenPageLimit(2);
        super.setupPagedToolbar(toolbar, viewPager);

        // Give the TabLayout the ViewPager
        tabLayout = root.findViewById(R.id.sliding_tabs);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case POS_RUNNING:
                    tab.setText(R.string.downloads_running_label);
                    break;
                case POS_COMPLETED:
                    tab.setText(R.string.downloads_completed_label);
                    break;
                case POS_LOG:
                    tab.setText(R.string.downloads_log_label);
                    break;
                default:
                    break;
            }
        }).attach();

        // restore our last position
        SharedPreferences prefs = getActivity().getSharedPreferences(TAG, Context.MODE_PRIVATE);
        int lastPosition = prefs.getInt(PREF_LAST_TAB_POSITION, 0);
        viewPager.setCurrentItem(lastPosition, false);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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

    public static class DownloadsPagerAdapter extends FragmentStateAdapter {

        DownloadsPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case POS_RUNNING:
                    return new RunningDownloadsFragment();
                case POS_COMPLETED:
                    return new CompletedDownloadsFragment();
                default:
                case POS_LOG:
                    return new DownloadLogFragment();
            }
        }

        @Override
        public int getItemCount() {
            return TOTAL_COUNT;
        }
    }
}
