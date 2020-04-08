package de.danoeh.antennapod.fragment.gpodnet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import de.danoeh.antennapod.R;

/**
 * Main navigation hub for gpodder.net podcast directory
 */
public class GpodnetMainFragment extends Fragment {

    private static final int NUM_PAGES = 2;
    private static final int POS_TOPLIST = 0;
    private static final int POS_TAGS = 1;
    private static final int POS_SUGGESTIONS = 2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.pager_fragment, container, false);
        Toolbar toolbar = root.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.gpodnet_main_label);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        ViewPager2 viewPager = root.findViewById(R.id.viewpager);
        GpodnetPagerAdapter pagerAdapter = new GpodnetPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = root.findViewById(R.id.sliding_tabs);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case POS_TAGS:
                    tab.setText(R.string.gpodnet_taglist_header);
                    break;
                case POS_TOPLIST:
                    tab.setText(R.string.gpodnet_toplist_header);
                    break;
                default:
                case POS_SUGGESTIONS:
                    tab.setText(R.string.gpodnet_suggestions_header);
                    break;
            }
        }).attach();

        return root;
    }

    public static class GpodnetPagerAdapter extends FragmentStateAdapter {

        GpodnetPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Bundle arguments = new Bundle();
            arguments.putBoolean(PodcastListFragment.ARGUMENT_HIDE_TOOLBAR, true);
            switch (position) {
                case POS_TAGS:
                    return new TagListFragment();
                case POS_TOPLIST:
                    PodcastListFragment topListFragment = new PodcastTopListFragment();
                    topListFragment.setArguments(arguments);
                    return topListFragment;
                default:
                case POS_SUGGESTIONS:
                    PodcastListFragment suggestionsFragment = new SuggestionListFragment();
                    suggestionsFragment.setArguments(arguments);
                    return suggestionsFragment;
            }
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
    }
}
