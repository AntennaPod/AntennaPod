package de.danoeh.antennapod.fragment.gpodnet;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.fragment.OnlineSearchFragment;
import de.danoeh.antennapod.net.discovery.GpodnetPodcastSearcher;

/**
 * Main navigation hub for gpodder.net podcast directory
 */
public class GpodnetMainFragment extends Fragment {

    private static final int NUM_PAGES = 2;
    private static final int POS_TOPLIST = 0;
    private static final int POS_TAGS = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.pager_fragment, container, false);
        setupToolbar(root.findViewById(R.id.toolbar));

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
                case POS_TOPLIST: // Fall-through
                default:
                    tab.setText(R.string.gpodnet_toplist_header);
                    break;
            }
        }).attach();

        return root;
    }

    private void setupToolbar(Toolbar toolbar) {
        toolbar.setTitle(R.string.gpodnet_main_label);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        toolbar.inflateMenu(R.menu.search);
        MenuItem searchItem = toolbar.getMenu().findItem(R.id.action_search);
        final SearchView sv = (SearchView) searchItem.getActionView();
        sv.setQueryHint(getString(R.string.gpodnet_search_hint));
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Activity activity = getActivity();
                if (activity != null) {
                    searchItem.collapseActionView();
                    ((MainActivity) activity).loadChildFragment(
                            OnlineSearchFragment.newInstance(GpodnetPodcastSearcher.class, query));
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
    }

    public static class GpodnetPagerAdapter extends FragmentStateAdapter {

        GpodnetPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case POS_TAGS:
                    return new TagListFragment();
                case POS_TOPLIST: // Fall-through
                default:
                    return new PodcastTopListFragment();
            }
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
    }
}
